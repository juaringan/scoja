// $Id: ReaderA.java,v 1.1 2003/02/07 11:17:37 mario_martinez Exp $

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

public class ReaderA {
    public static void main(String[] args) throws Exception {
	int i = 0;
	final int port = Integer.parseInt(args[i++]);
	
	final ServerSocket server = new ServerSocket(port);
	
	while(true) {
	    try {
		System.out.print("Waiting...");
		final Socket insocket = server.accept();
		System.out.println("received connection.");
		//inchannel.configureBlocking(false);
		replyData(insocket);
	    } catch(Exception ex) {
		ex.printStackTrace(System.out);
	    }
	}
    }
    
    static void replyData(Socket insocket)
	throws Exception {
	System.out.println("Reading for a socket" );
	final byte[] buffer = new byte[10];
	final InputStream in = insocket.getInputStream();
	int total = 0;
	int readed = in.read(buffer);
	while (readed > 0) {
	    System.out.println("Data:");
	    for (int i = 0; i < readed; i++) {
		System.out.print((char)buffer[i]);
	    }
	    System.out.println();
	    total += readed;
	    readed = in.read(buffer);
	}
	System.out.println("Total readed " + total);
	insocket.close();
    }
}
