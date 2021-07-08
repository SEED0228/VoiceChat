import java.net.*;
import java.util.*;
import java.io.*;

public class VCServer {
	public static void main(String[] args) {
		VCServer application = VCServer.getInstance();
		application.start();
	}

	private static VCServer instance;
	public static VCServer getInstance() {
		if (instance == null) {
			instance = new VCServer();
		}
		return instance;
	}

	private ServerSocket server;

	private ArrayList<VCRoom> roomList;

	private ArrayList<VCUser> userList;

	private VCServer() {
		roomList = new ArrayList<VCRoom>();
		userList = new ArrayList<VCUser>();
	}

	public void start() {
		try {
			server = new ServerSocket(2815);

			while(!server.isClosed()) {
				Socket client = server.accept();

				VCUser user = new VCUser(client);
				addUser(user);
			}
		}
		catch(Exception err) {
			err.printStackTrace();
		}
	}

	public void addVCRoom(VCRoom room) {
		if (roomList.contains(room)) return;

		roomList.add(room);
System.out.println("addRoom=[" + room + "]");

		for(int i = 0 ; i < userList.size() ; i++) {
			userList.get(i).reachedMessage(new VCContainer(Command.GetRooms));
		}
	}
	public VCRoom getVCRoom(String name) {
		for(int i = 0 ; i < roomList.size() ; i++) {
			VCRoom room = roomList.get(i);
			if (name.equals(room.getName())) return room;
		}
		return null;
	}

	public VCRoom[] getVCRooms() {
		VCRoom[] result = new VCRoom[roomList.size()];

		for(int i = 0 ; i < roomList.size() ; i++) {
			result[i] = roomList.get(i);
		}
		return result;
	}
	public void removeVCRoom(VCRoom room) {
		roomList.remove(room);
System.out.println("removeRoom=[" + room + "]");

		for(int i = 0 ; i < userList.size() ; i++) {
			userList.get(i).reachedMessage(new VCContainer(Command.GetRooms));
		}
	}

	public void clearVCRoom() { 
		roomList.clear();

		for(int i = 0 ; i < userList.size() ; i++) {
			userList.get(i).reachedMessage(new VCContainer(Command.GetRooms));
		}
	}

	public void addUser(VCUser user) {
		if (userList.contains(user)) return;

		userList.add(user);
System.out.println("addUser=[" + user + "]");
	}

	public VCUser getUser(String name) {
		for(int i = 0 ; i < userList.size() ; i++) {
			VCUser user = userList.get(i);
			if (user.getName().equals(name)) return user;
		}
		return null;
	}

	public VCUser[] getUsers() {
		VCUser[] users = new VCUser[userList.size()];
		userList.toArray(users);
		return users;
	}

	public void removeUser(VCUser user) {
		userList.remove(user);
		System.out.println("removeUser=[" + user + "]");

		for(int i = 0 ; i < roomList.size() ; i++) {
			if (roomList.get(i).containsUser(user)) roomList.get(i).removeUser(user);
		}
	}

	public void clearUser() { userList.clear(); }

	public void close() throws IOException {
		server.close();
	}
}

class VCRoom implements VCListener {

	private String name;

	private VCUser hostUser;

	private ArrayList<VCUser> roomUsers;

	public VCRoom(String name, VCUser hostUser) {
		roomUsers = new ArrayList<VCUser>();

		this.name = name;
		this.hostUser = hostUser;

		addUser(hostUser);
	}

	public String getName() {
		return name;
	}

	public VCUser getHostUser() {
		return hostUser;
	}

	public void addUser(VCUser user) {
		user.addVCListener(this);
		roomUsers.add(user);
		for(int i = 0 ; i < roomUsers.size() ; i++) {
			roomUsers.get(i).reachedMessage(new VCContainer(Command.GetUsers, name));
			roomUsers.get(i).sendMessage(new VCContainer(Command.SubmitMessage, "enter " + user.getName()));
		}
	}

	public boolean containsUser(VCUser user) {
		return roomUsers.contains(user);
	}

	public VCUser[] getUsers() {
		VCUser[] users = new VCUser[roomUsers.size()];
		roomUsers.toArray(users);
		return users;
	}

	public void removeUser(VCUser user) {
		user.removeVCListener(this);
		roomUsers.remove(user);
		for(int i = 0 ; i < roomUsers.size() ; i++) {
			roomUsers.get(i).reachedMessage(new VCContainer(Command.GetUsers, name));
			roomUsers.get(i).sendMessage(new VCContainer(Command.SubmitMessage, "Exit " + user.getName()));
		}

		if (roomUsers.size() == 0) {
			VCServer.getInstance().removeVCRoom(this);
		}
	}

	public void messageThrow(VCEvent e) {
		VCUser source = e.getUser();
		VCContainer vcCon = e.getVCContainer();
		Command cmd = vcCon.command;
		System.out.println("room:throw:cmd:"+cmd+"cnt"+roomUsers.size());
		switch(cmd) {
			case SubmitMessage :
				for(int i = 0 ; i < roomUsers.size() ; i++) {
					String message = source.getName() + ">" + e.getVCContainer().argStr1;
					roomUsers.get(i).sendMessage(new VCContainer(Command.SubmitMessage, message));
				}
				break;
				case SubmitVoice :
				for(int i = 0 ; i < roomUsers.size() ; i++) {
					String messageNum = source.getName() + e.getVCContainer().argStr1;
					String message = ">" + source.getName() + " sent voice (" + source.getName() + e.getVCContainer().argStr1 + ")";
					roomUsers.get(i).sendMessage(new VCContainer(Command.SubmitVoice, messageNum, message,  e.getVCContainer().data));
				}
				break;
			case SetName :
				for(int i = 0 ; i < roomUsers.size() ; i++) {
					roomUsers.get(i).reachedMessage(new VCContainer(Command.GetUsers, name));
				}
				break;
			default :
				break;
		}
	}
}


interface VCListener extends EventListener {
	void messageThrow(VCEvent e);
}

class VCUser implements Runnable, VCListener {

	private Socket socket;

	private String name;

	ObjectOutputStream oos;

	private VCServer server = VCServer.getInstance();

	private ArrayList<VCListener> VCListeners;

	public VCUser(Socket socket) {
		VCListeners = new ArrayList<VCListener>();
		this.socket = socket;

		addVCListener(this);

		Thread thread = new Thread(this);
		thread.start();
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return this.name;
	}

	public void run() {
		try {

			InputStream input = socket.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(input);
			OutputStream output = socket.getOutputStream();
			oos = new ObjectOutputStream(output);

			while(!socket.isClosed()) {
				VCContainer vcCon = (VCContainer) ois.readObject();
				System.out.println("arg1:"+vcCon.argStr1);
				System.out.println("arg2"+vcCon.argStr2);
				System.out.println("INPUT=" + vcCon.command);

				reachedMessage(vcCon);
			}
		}
		catch(Exception err) { err.printStackTrace(); }
	}

	public void messageThrow(VCEvent e) {
		VCContainer vcCon = e.vcCon;
		String name, result;
		String value = vcCon.argStr1;
		System.out.println("usr:throw:arg1:"+value);
		VCRoom room;
		Command cmd = vcCon.command;
		System.out.println("usr:throw:cmd:"+vcCon.command);
		switch(cmd) {
			case Close:
				try { close(); }
				catch(IOException err) { err.printStackTrace(); }
				break;
			case SetName:
				name = vcCon.argStr1;
				if (name.indexOf(" ") == -1) {
					String before = getName();
					setName(name);
					sendMessage(new VCContainer(Command.Success, "setName"));
					reachedMessage(new VCContainer(Command.SubmitMessage, "rename " + before + " to " + name));
				}
				else {
					sendMessage(new VCContainer(Command.Error, "invalid name"));
				}
				break;
			case AddRoom:
				name = vcCon.argStr1;
				if (name.indexOf(" ") == -1) {
					room = new VCRoom(name , this);
					server.addVCRoom(room);
					this.reachedMessage(new VCContainer(Command.GetUsers, name));
					sendMessage(new VCContainer(Command.Success, "addRoom"));
				}
				else {
					sendMessage(new VCContainer(Command.Error, "invalid name"));
				}
				break;
			case GetRooms:
				result = "";
				VCRoom[] rooms = server.getVCRooms();
				for(int i = 0 ; i < rooms.length ; i++) {
					result += rooms[i].getName() + " ";
				}
				sendMessage(new VCContainer(Command.GetRooms, result));
				break;
			case EnterRoom:
				value = vcCon.argStr1;
				room = server.getVCRoom(value);
				if (room != null) {
					room.addUser(this);
					sendMessage(new VCContainer(Command.Success, "enterRoom"));
				}
				else {
					sendMessage(new VCContainer(Command.Error, "cannot find " + value));
				} 
				break;
			case ExitRoom:
				value = vcCon.argStr1;
				room = server.getVCRoom(value);
				if (room != null) {
					room.removeUser(this);
					sendMessage(new VCContainer(Command.Success, "exitRoom"));

				}
				else {
					sendMessage(new VCContainer(Command.Error, "cannot find " + value));
				} 
				break;
			case GetUsers:
				value = vcCon.argStr1;
				room = server.getVCRoom(value);
				if (room != null) {
					result = "";
					VCUser[] users = room.getUsers();
					for(int i = 0 ; i < users.length ; i++) {
						result += users[i].getName() + " ";
					}
					sendMessage(new VCContainer(Command.GetUsers, result));
				}
				break;
			default:
				break;
		}
	}


	public void close() throws IOException {
		server.removeUser(this);
		VCListeners.clear();
		socket.close();
	}

	public void sendMessage(VCContainer vcCon) {
		try {
			
			System.out.println("send:" + vcCon.command);

			oos.writeObject(vcCon);
			oos.flush();
		}
		catch(Exception err) {
		}
	}

	public void reachedMessage(VCContainer vcCon) {
		VCEvent event = new VCEvent(this, vcCon);
		for(int i = 0 ; i < VCListeners.size() ; i++ ) {
			VCListeners.get(i).messageThrow(event);
		}
	}

	public void addVCListener(VCListener l) {
		VCListeners.add(l);
	}

	public void removeVCListener(VCListener l) {
		VCListeners.remove(l);
	}

	public VCListener[] getVCListeners() {
		VCListener[] listeners = new VCListener[VCListeners.size()];
		VCListeners.toArray(listeners);
		return listeners;
	}
}