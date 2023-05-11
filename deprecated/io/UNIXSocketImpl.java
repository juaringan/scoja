package org.scoja.util;

import org.scoja.util.UNIXSocketInputStream;
import org.scoja.util.UNIXSocketOutputStream;

import java.net.SocketException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;


/**
 * Unix socket native level access.
 */
public abstract class UNIXSocketImpl {

    protected abstract void socket(short type)throws SocketException;

    protected abstract void bind(UNIXAddress addr) 
	throws SocketException, 
	       IllegalArgumentException;

    protected abstract void listen(final int queueLength) 
	throws SocketException;

    protected abstract UNIXSocket accept() throws SocketException;

    protected abstract void connect(UNIXAddress addr) 
	throws SocketException, 
	       IllegalArgumentException;

    protected abstract void close() throws SocketException;

    protected abstract void send(byte[] msg) throws IOException;

    protected abstract int receive(byte[] msg) throws IOException;

    protected abstract void setFileDescriptor(int fd);
}
