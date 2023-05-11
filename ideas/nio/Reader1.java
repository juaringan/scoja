// $Id: Reader1.java,v 1.2 2003/05/13 10:09:43 elmartinfierro Exp $

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

public class Reader1 {
    public static void main(String[] args) throws Exception {
	int i = 0;
	final int port = Integer.parseInt(args[i++]);
	
	final Selector selector = Selector.open();
	final ServerSocketChannel server = ServerSocketChannel.open();
	server.configureBlocking(false);
	server.socket().bind(new InetSocketAddress(port));
	server.register(selector, SelectionKey.OP_ACCEPT);
	
	while(true) {
	    try {
		System.out.print("Waiting...");
		selector.select();
		reply(selector);
	    } catch(Exception ex) {
		ex.printStackTrace(System.out);
	    }
	}
    }
    
    static void reply(Selector selector) throws Exception {
	final Set readyKeys = selector.selectedKeys();
        System.out.println("received: " + readyKeys);
	final Iterator rkit = readyKeys.iterator();
	while(rkit.hasNext()) {
	    final SelectionKey key = (SelectionKey)rkit.next();
	    rkit.remove();
	    final Channel channel = key.channel();
	    
	    if (channel instanceof ServerSocketChannel) {
		replyConnection(selector, (ServerSocketChannel)channel);
	    } else if (channel instanceof SocketChannel) {
		replyData(selector, (SocketChannel)channel);
	    } else {
		throw new Exception("Unknown selectable item " + channel);
	    }
	}
    }
    
    static void replyConnection(Selector selector, ServerSocketChannel server)
	throws Exception {
	System.out.println("Adding a new channel");
	final SocketChannel inchannel = server.accept();
	inchannel.configureBlocking(false);
	inchannel.register(selector, SelectionKey.OP_READ);
    }

    /*    
    static void replyData(Selector selector, SocketChannel channel)
	throws Exception {
	final InputStream is = channel.socket().getInputStream();
	System.out.println("Reading for a channel with " 
			   + is.available() + " available bytes");
	final byte[] buffer = new byte[1024];
	int total = 0;
	int readed = is.read(buffer);
	while (readed > 0) {
	    for (int i = 0; i < readed; i++) System.out.print((char)buffer[i]);
	    total += readed;
	    readed = is.read(buffer);
	}
    }
    */
    
    static void replyData(Selector selector, SocketChannel channel)
	throws Exception {
	System.out.println("Reading for the channel " + channel
			   + " " + channel.isConnected());
	final ByteBuffer buffer = ByteBuffer.allocate(10);
	int total = 0;
	int readed = channel.read(buffer);
	while (readed > 0) {
	    buffer.flip();
	    for (int i = 0; i < buffer.limit(); i++) {
		System.out.print((char)buffer.get(i));
	    }
	    total += readed;
	    readed = channel.read(buffer);
	}
	System.out.println("Total readed " + total 
			   + ". Last readed " + readed);
	if (readed < 0) channel.close();
    }
}
