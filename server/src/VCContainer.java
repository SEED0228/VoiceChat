import java.io.Serializable;
import java.nio.ByteBuffer;

public class VCContainer implements Serializable{

  public String argStr1;
  public Command command;
  public String argStr2;
  byte [] data = new byte[10000];

  public static void main(String[] args){
    VCContainer vcCon = new VCContainer();
    System.out.println(""+vcCon.getArgStr1());
    System.out.println(""+vcCon.getArgStr2());
    System.out.println(""+vcCon.getCommand());

  }

  VCContainer(){
    this.command = Command.None;
    this.argStr1 = "none";
    this.argStr2 = "none";
    // ByteBuffer byteBuffer = ByteBuffer.wrap(this.data);
    // int x = 0;
    // byteBuffer.putInt(0, x);
  }

  VCContainer(Command cmd){
    this.command = cmd;
    this.argStr1 = "none";
    this.argStr2 = "none";
    // ByteBuffer byteBuffer = ByteBuffer.wrap(this.data);
    // int x = 0;
    // byteBuffer.putInt(0, x);
  }

  VCContainer(Command cmd, byte data[]){
    this.command = cmd;
    this.argStr1 = "none";
    this.argStr2 = "none";
    this.data = data;
  }

  VCContainer(Command cmd, String arg1, byte data[]){
    this.command = cmd;
    this.argStr1 = arg1;
    this.argStr2 = "none";
    this.data = data;
  }

  VCContainer(Command cmd, String arg1, String arg2, byte data[]){
    this.command = cmd;
    this.argStr1 = arg1;
    this.argStr2 = arg2;
    this.data = data;
  }
  
  VCContainer(Command cmd, String arg1){
    this.command = cmd;
    this.argStr1 = arg1;
    this.argStr2 = "none";
    // ByteBuffer byteBuffer = ByteBuffer.wrap(this.data);
    // int x = 0;
    // byteBuffer.putInt(0, x);
  }

  VCContainer(Command cmd, String arg1, String arg2){
    this.command = cmd;
    this.argStr1 = arg1;
    this.argStr2 = arg2;
    // ByteBuffer byteBuffer = ByteBuffer.wrap(this.data);
    // int x = 0;
    // byteBuffer.putInt(0, x);
  }

  public String getArgStr1() {
    return this.argStr1;
  }

  public String getArgStr2() {
    return this.argStr2;
  }

  public Command getCommand() {
    return this.command;
  }

}