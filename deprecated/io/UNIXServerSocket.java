
package org.scoja.util;


import java.net.SocketException;
import java.io.IOException;

/**
 * Unix Server socket implementation.
 */
public final class UNIXServerSocket {

    private final static short SOCK_TYPE = 1; // "SOCK_STREAM"

    private UNIXSocketImpl impl;


    public UNIXServerSocket() throws SocketException {
	//	impl = new @@TYPE@@;
	impl = new LinuxSocket();
	impl.socket(SOCK_TYPE);
    }

    public final void bind(UNIXAddress addr) 
	throws SocketException, 
	       IllegalArgumentException {
	
	impl.bind(addr);
    }

    public final void listen(final int queueLength) 
	throws SocketException {

	impl.listen(queueLength);
    }

    public final void close() throws SocketException {
	impl.close();
    }

    // ¿me llevaría el código de este método a la clase Impl?
    public final UNIXSocket accept() throws SocketException {
	return impl.accept();
    }

}
