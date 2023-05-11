// $Id: ChannelCopy.java,v 1.2 2008/04/09 06:54:18 pedropalao-bk Exp $

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * A simple cp implemented with NIO.
 */
public class ChannelCopy {
    public static void main(String[] args) throws Exception {
	int argc = 0;
	final String inname = args[argc++];
	final String outname = args[argc++];
	final FileInputStream instream = new FileInputStream(inname);
	final FileChannel inchannel = instream.getChannel();
	final FileOutputStream outstream = new FileOutputStream(outname);
	final FileChannel outchannel = outstream.getChannel();
	
	final ByteBuffer bb = ByteBuffer.allocate(10);
	int readed = inchannel.read(bb);
	System.out.println("Readed " + readed + " bytes."
			   + " Position at " + bb.position());
	while (readed > 0) {
	    bb.flip();
	    outchannel.write(bb);
	    bb.clear();
	    System.out.println("Position at " + bb.position());
	    readed = inchannel.read(bb);
	    System.out.println("Readed " + readed + " bytes."
			       + " Position at " + bb.position());
	}
	outstream.close();
	instream.close();
    }
}
