// $Id: ChannelCat.java,v 1.2 2008/04/09 06:54:18 pedropalao-bk Exp $

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * A simple cat implemented with NIO.
 */
public class ChannelCat {
    public static void main(String[] args) throws Exception {
	int argc = 0;
	final String filename = args[argc++];
	final FileInputStream filestream = new FileInputStream(filename);
	final FileChannel filechannel = filestream.getChannel();
	final ByteBuffer bb = ByteBuffer.allocate(10);
	int readed = filechannel.read(bb);
	while (readed > 0) {
	    System.out.println("Readed " + readed + " bytes."
			       + " Position at " + bb.position());
	    bb.flip();
	    System.out.println("Data:");
	    for (int i = 0; i < bb.limit(); i++) {
		System.out.print((char)bb.get(i));
	    }
	    System.out.println();
	    System.out.println("Position at " + bb.position());
	    readed = filechannel.read(bb);
	}
    }
}
