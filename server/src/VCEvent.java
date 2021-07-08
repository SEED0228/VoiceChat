import java.net.*;
import java.util.*;
import java.io.*;

public class VCEvent extends EventObject {
	private VCUser source;

	public VCContainer vcCon;

	public VCEvent(VCUser source, VCContainer vcCon) {
		super(source);
		this.source = source;
		this.vcCon = vcCon;
		System.out.println("cons:command:"+vcCon.getCommand());
	}

	public VCUser getUser() { return source; }

	public VCContainer getVCContainer() { return this.vcCon; }

}