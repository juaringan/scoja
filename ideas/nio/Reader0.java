// $Id: Reader0.java,v 1.1 2003/02/07 11:17:37 mario_martinez Exp $

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

public class Reader0 {
    public static void main(String[] args) throws Exception {
	int i = 0;
	final int port = Integer.parseInt(args[i++]);
	
	final ServerSocketChannel server = ServerSocketChannel.open();
	server.socket().bind(new InetSocketAddress(port));
	
	while(true) {
	    try {
		System.out.print("Waiting...");
		final SocketChannel inchannel = server.accept();
		System.out.println("received connection.");
		//inchannel.configureBlocking(false);
		replyData(inchannel);
	    } catch(Exception ex) {
		ex.printStackTrace(System.out);
	    }
	}
    }
    
    static void replyData(SocketChannel channel)
	throws Exception {
	System.out.println("Reading for a channel" );
	final ByteBuffer buffer = ByteBuffer.allocate(10000);
	int total = 0;
	int readed = channel.read(buffer);
	while (readed > 0) {
	    buffer.flip();
	    System.out.println("Data:");
	    for (int i = 0; i < buffer.limit(); i++) {
		System.out.print((char)buffer.get(i));
	    }
	    System.out.println();
	    total += readed;
	    //buffer.reset();
	    readed = channel.read(buffer);
	}
	System.out.println("Total readed " + total);
	channel.close();
    }
}
