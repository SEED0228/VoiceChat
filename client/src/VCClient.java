import java.util.*;
import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.sound.sampled.*;

public class VCClient extends JFrame implements Runnable, ActionListener {
	// public static void main(String[] args) {
	// 	VCClient window = new VCClient("localhost");
	// 	window.setSize(1350, 600);
	// 	window.setVisible(true);
	// }

	private static final String APPNAME = "Voice Chat";

	public static String HOST;//"localhost";//"192.168.11.11";

	private static final int PORT = 2815;

	private Socket socket;

	private Thread thread;

	private String roomName;

	private ObjectOutputStream oos;

	private JList roomList;
	private JList userList;	
	private JTextArea msgTextArea;		
	private JTextField msgTextField;	
	private JTextField nameTextField;	
	private JTextField secondTextField;	
	private JButton submitButton;		
	private JButton renameButton;		
	private JButton addRoomButton;		
	private JButton enterRoomButton;

	private JButton recordButton;
	private JButton playButton;
	private JList vcList;
	private int vcCount = 0;
	private byte[] data;


	float SAMPLE_RATE = 44100;
	int SAMPLE_SIZE_IN_BITS = 16;
	int CHANNELS = 2;
	boolean SIGNED = true;
	boolean BIG_ENDIAN = true;
	AudioFormat format;
	TargetDataLine line;
	AudioInputStream ais;
	
	public ArrayList<byte[]> voiceData = new ArrayList<>();
	public ArrayList<String> voiceString = new ArrayList<>();

	public VCClient(String host) {
		super(APPNAME);
		this.HOST = host;

		JPanel topPanel = new JPanel();
		JPanel leftPanel = new JPanel();
		JPanel buttomPanel = new JPanel();

		JPanel roomPanel = new JPanel();
		JPanel userPanel = new JPanel();

		JPanel vcPanel = new JPanel();
		JPanel vcsetPanel = new JPanel();
		JPanel otherPanel = new JPanel();

		roomList = new JList();
		userList = new JList();
		vcList = new JList();
		msgTextArea = new JTextArea();
		msgTextField = new JTextField();
		nameTextField = new JTextField();
		secondTextField = new JTextField();
		submitButton = new JButton("Send");
		renameButton = new JButton("Rename");
		addRoomButton = new JButton("Add Room");
		enterRoomButton = new JButton("Enter");
		recordButton = new JButton("Record");
		playButton = new JButton("Play");

		submitButton.addActionListener(this);
		submitButton.setActionCommand("submit");

		renameButton.addActionListener(this);
		renameButton.setActionCommand("rename");

		addRoomButton.addActionListener(this);
		addRoomButton.setActionCommand("addRoom");

		enterRoomButton.addActionListener(this);
		enterRoomButton.setActionCommand("enterRoom");

		recordButton.addActionListener(this);
		recordButton.setActionCommand("record");


		playButton.addActionListener(this);
		playButton.setActionCommand("play");

		roomPanel.setLayout(new BorderLayout());
		roomPanel.add(new JLabel("Chat Room"), BorderLayout.NORTH);
		roomPanel.add(new JScrollPane(roomList), BorderLayout.CENTER);
		roomPanel.add(enterRoomButton, BorderLayout.SOUTH);

		userPanel.setLayout(new BorderLayout());
		userPanel.add(new JLabel("Entering User"), BorderLayout.NORTH);
		userPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

		topPanel.setLayout(new FlowLayout());
		topPanel.add(new JLabel("Name"));
		topPanel.add(nameTextField);
		topPanel.add(renameButton);
		topPanel.add(addRoomButton);

		nameTextField.setPreferredSize(new Dimension(200, nameTextField.getPreferredSize().height));

		leftPanel.setLayout(new GridLayout(2, 1));
		leftPanel.add(roomPanel);
		leftPanel.add(userPanel);

		buttomPanel.setLayout(new BorderLayout());
		buttomPanel.add(msgTextField, BorderLayout.CENTER);
		buttomPanel.add(submitButton, BorderLayout.EAST);

		msgTextArea.setEditable(false);

		exitedRoom();

		// this.getContentPane().add(new JScrollPane(msgTextArea), BorderLayout.CENTER);
		// this.getContentPane().add(topPanel, BorderLayout.NORTH);
		// this.getContentPane().add(leftPanel, BorderLayout.WEST);
		// this.getContentPane().add(buttomPanel, BorderLayout.SOUTH);
		
		otherPanel.setLayout(new BorderLayout());
		otherPanel.add(new JScrollPane(msgTextArea), BorderLayout.CENTER);
		otherPanel.add(topPanel, BorderLayout.NORTH);
		otherPanel.add(leftPanel, BorderLayout.WEST);
		otherPanel.add(buttomPanel, BorderLayout.SOUTH);

		vcPanel.setLayout(new BorderLayout());
		vcPanel.add(new JScrollPane(vcList), BorderLayout.CENTER);
		vcPanel.add(new JLabel("VoiceChat"), BorderLayout.NORTH);
		vcsetPanel.setLayout(new FlowLayout());
		vcsetPanel.add(secondTextField);
		vcsetPanel.add(new JLabel("sec"));
		vcsetPanel.add(recordButton);
		vcsetPanel.add(playButton);
		vcPanel.add(vcsetPanel, BorderLayout.SOUTH);
		secondTextField.addActionListener(new RecordActionListener());
		secondTextField.setPreferredSize(new Dimension(50, secondTextField.getPreferredSize().height));

		this.getContentPane().add(otherPanel, BorderLayout.CENTER);
		this.getContentPane().add(vcPanel, BorderLayout.EAST);
		

		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				try { close(); }
				catch(Exception err) { }
			}
		});

		connectServer();

		thread = new Thread(this);
		thread.start();

		sendMessage(new VCContainer(Command.GetRooms));
		
	}

	class RecordActionListener implements ActionListener
    {
         
        //テキストフィールドでEnterを押されたさいの処理
        //1秒、入力された周波数の音を流す
        public void actionPerformed(ActionEvent e)
        {
            //テキストの値が正常か調べる
            JTextField tmp=(JTextField)e.getSource();
            int a;
            try{
                a=Integer.parseInt(tmp.getText());
            }catch(Exception er){
                System.out.println("error:"+er);
                return;
            }
            try{
                AudioFormat linear=new AudioFormat(44100,8,1,true,false);
                DataLine.Info info=new DataLine.Info(TargetDataLine.class,linear);
                TargetDataLine target=(TargetDataLine)AudioSystem.getLine(info);
                target.open(linear);
                target.start();
                AudioInputStream stream=new AudioInputStream(target);

                data=new byte[44100*8/8*1*a];
                stream.read(data,0,data.length);

                target.stop();
                target.close();
				
				sendMessage(new VCContainer(Command.SubmitVoice, "" + vcCount, data));
				vcCount++;
                
                
            }catch(Exception er){
                System.out.println("error:"+er);
            }
        }
    }

	public void connectServer() {
		try {
			socket = new Socket(HOST, PORT);
			msgTextArea.append(">Successful server connection\n"); 
			oos = new ObjectOutputStream(socket.getOutputStream());
		}
		catch(Exception err) {
			msgTextArea.append("ERROR>" + err + "\n"); 
			System.out.println("ERROR>" + err + "\n");
		}
	}

	public void close() throws IOException {
		socket.close();
	}

	public void sendMessage(String msg) {
		try {
			OutputStream output = socket.getOutputStream();
			PrintWriter writer = new PrintWriter(output);

			writer.println(msg);
			writer.flush();
		}
		catch(Exception err) { msgTextArea.append("ERROR>" + err + "\n"); }
	}

	public void sendMessage(VCContainer vcCon) {
		try {
			oos.writeObject(vcCon);
			oos.flush();
		}
		catch(Exception err) { msgTextArea.append("ERROR>" + err + "\n"); }
	}


	public void run() {
		try {
			InputStream input = socket.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(input);
			while(!socket.isClosed()) {
				VCContainer vcCon = (VCContainer) ois.readObject();

				reachedMessage(vcCon);
			}
		}
		catch(Exception err) { }
	}

	public void reachedMessage(VCContainer vcCon) {
		System.out.println(vcCon.command);
		switch (vcCon.command) {
			case GetRooms:
				if(vcCon.argStr1 .equals("")) {
					roomList.setModel(new DefaultListModel());
				}
				else {
					String[] rooms = vcCon.argStr1.split(" ");
					roomList.setListData(rooms);
				}
				break;
			case GetUsers:
				if(vcCon.argStr1.equals("")) {
					userList.setModel(new DefaultListModel());
				}
				else {
					String[] users = vcCon.argStr1.split(" ");
					userList.setListData(users);
				}
				break;
			case SubmitMessage:
				msgTextArea.append(vcCon.argStr1 + "\n"); 
				break;
			case SubmitVoice:
				voiceData.add(vcCon.data);
				voiceString.add(vcCon.argStr1);
				vcList.setListData(voiceString.toArray(new String[voiceString.size()])); 
				msgTextArea.append(vcCon.argStr2 + "\n"); 
				break;
			case Success:
				if(vcCon.argStr1.equals("setName")){
					msgTextArea.append(">rename successfully\n"); 
				}
				break;
			case Error:
				msgTextArea.append("ERROR>" + vcCon.argStr1 + "\n"); 
				break;
			default :
				break;
		}
		
	}

	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();

		if(cmd.equals("submit")) {	
			sendMessage(new VCContainer(Command.SubmitMessage, msgTextField.getText()));
			msgTextField.setText("");
		}
		else if(cmd.equals("rename")) {
			sendMessage(new VCContainer(Command.SetName, nameTextField.getText()));
		}
		else if(cmd.equals("addRoom")) {	
			String roomName = nameTextField.getText();
			sendMessage(new VCContainer(Command.AddRoom, roomName));
			enteredRoom(roomName);
		}
		else if(cmd.equals("enterRoom")) {	
			Object room = roomList.getSelectedValue();
			if (room != null) {
				String roomName = room.toString();
				sendMessage(new VCContainer(Command.EnterRoom, roomName));
				enteredRoom(roomName);
			}
		}
		else if(cmd.equals("exitRoom")) {	
			sendMessage(new VCContainer(Command.ExitRoom, roomName));
			exitedRoom();
		}
		else if(cmd.equals("record")) {	
			recordVoice();
		}
		else if(cmd.equals("play")) {	
			playVoice();
		}
	}

	private void recordVoice() {
		int a;
		try{
			a=Integer.parseInt(secondTextField.getText());
		}catch(Exception er){
			msgTextArea.append(">invalid input(second)\n"); 
			System.out.println("error:"+er);
			return;
		}
		try{
			AudioFormat linear=new AudioFormat(44100,8,1,true,false);
			DataLine.Info info=new DataLine.Info(TargetDataLine.class,linear);
			TargetDataLine target=(TargetDataLine)AudioSystem.getLine(info);
			target.open(linear);
			target.start();
			AudioInputStream stream=new AudioInputStream(target);

			data=new byte[44100*8/8*1*a];
			stream.read(data,0,data.length);

			target.stop();
			target.close();
			
			sendMessage(new VCContainer(Command.SubmitVoice, "" + vcCount, data));
			vcCount++;
			
			
		}catch(Exception er){
			System.out.println("error:"+er);
		}
	}

	private void  playVoice(){
		int index = vcList.getMinSelectionIndex();
		if (index != -1) {
			try {
				AudioFormat frmt=new AudioFormat(44100,8,1,true,false);
				DataLine.Info info2=new DataLine.Info(Clip.class,frmt);
				Clip clip=(Clip)AudioSystem.getLine(info2);
				clip.open(frmt,voiceData.get(index),0,voiceData.get(index).length);
				//再生を開始する
				clip.start();
				//再生が終わるまでループする
				while(clip.isRunning()){
					Thread.sleep(100);
				}
			}catch(Exception er){
				System.out.println("error:"+er);
			}
		}
	}

	private void enteredRoom(String roomName) {
		this.roomName = roomName;
		setTitle(APPNAME + " " + roomName);

		msgTextField.setEnabled(true);
		submitButton.setEnabled(true);
		recordButton.setEnabled(true);
		playButton.setEnabled(true);

		addRoomButton.setEnabled(false);
		enterRoomButton.setText("Exit");
		enterRoomButton.setActionCommand("exitRoom");
		// count
		vcCount = 1;
		voiceData = new ArrayList<>();
		voiceString = new ArrayList<>();
	}


	private void exitedRoom() {
		roomName = null;
		setTitle(APPNAME);

		msgTextField.setEnabled(false);
		submitButton.setEnabled(false);
		recordButton.setEnabled(false);
		playButton.setEnabled(false);

		addRoomButton.setEnabled(true);
		enterRoomButton.setText("Enter Room");
		enterRoomButton.setActionCommand("enterRoom");
		userList.setModel(new DefaultListModel());
		voiceData = new ArrayList<>();
		voiceString = new ArrayList<>();
		vcList.setModel(new DefaultListModel());
	}
}

enum Command {
	Close,
	SetName,
	AddRoom,
	GetRooms,
	EnterRoom,
	ExitRoom,
	GetUsers,
	SubmitMessage,
	SubmitVoice,
	Success,
	Error,
	None;
}