package org.scoja.util;


import java.net.SocketException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;


public final class LinuxSocket extends UNIXSocketImpl {

    static {
    //	System.loadLibrary(@@TYPE@@);
	System.loadLibrary("LinuxSocket");
    }

    private int fd;

    protected final void setFileDescriptor(int fd) {
	this.fd = fd;
    }

    protected final native void socket(short type) 
	throws SocketException;

    protected final native void bind(UNIXAddress addr) 
	throws SocketException, 
	       IllegalArgumentException;

    protected final native int native_accept() 
	throws SocketException;

    protected final UNIXSocket accept() throws SocketException {
	int cli_fd = native_accept();

	return new UNIXSocket(cli_fd);
    } 

    protected final native void listen(final int queueLength) 
	throws SocketException;

    protected final native void connect(UNIXAddress addr) 
	throws SocketException, 
	       IllegalArgumentException;

    protected final native void close() throws SocketException;

    protected final native void send(byte[] msg) throws IOException;

    protected final native int receive(byte[] msg) 
	throws IOException;


}
