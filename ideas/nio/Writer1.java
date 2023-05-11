// $Id: Writer1.java,v 1.1 2003/02/07 11:17:37 mario_martinez Exp $

import java.io.*;
import java.net.*;

public class Writer1 {
    public static void main(String[] args) throws Exception {
	int argc = 0;
	final String hostname = args[argc++];
	final int port = Integer.parseInt(args[argc++]);
	
	final InetAddress hostaddr = InetAddress.getByName(hostname);
	final Socket socket = new Socket(hostaddr, port);
	final PrintWriter out = new PrintWriter(socket.getOutputStream());
	
	boolean delay = true;
	while (argc < args.length) {
	    if (delay) {
		final long millis = Long.parseLong(args[argc]);
		Thread.currentThread().sleep(millis);
	    } else {
		out.println(args[argc]);
		out.flush();
	    }
	    argc++;
	    delay = !delay;
	}
	
	socket.close();
    }
}
