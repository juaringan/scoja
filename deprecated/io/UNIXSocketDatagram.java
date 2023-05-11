
package org.scoja.util;


import java.net.SocketException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

/**
 * Unix socket implementation.
 */
public final class UNIXSocketDatagram {

    private final static short SOCK_TYPE = 2; //"SOCK_DGRAM"

    private final UNIXSocketImpl impl;


    public UNIXSocketDatagram() throws SocketException {
	//	impl = new @@TYPE@@;
	impl = new LinuxSocket();
	impl.socket(SOCK_TYPE);
    }


    public final void bind(UNIXAddress addr) 
	throws SocketException, 
	       IllegalArgumentException {

	impl.bind(addr);
    }

    public final int receive(byte[] msg) throws IOException {
	return impl.receive(msg);
    }

    public final void send(byte[] msg) throws IOException {
	impl.send(msg);
    }

    public final void close() throws SocketException {
	impl.close();
    }

}
