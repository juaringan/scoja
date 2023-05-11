
package org.scoja.util;


import java.net.SocketException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

/**
 * Unix socket implementation.
 */
public final class UNIXSocket {

    //
    private final short SOCK_TYPE = 1; // SOCK_STREAM

    // implementación según sea Solaris, AIX, Linux...
    UNIXSocketImpl impl;


    public UNIXSocket() throws SocketException {
	//	impl = new @@IMPL@@;
	impl = new LinuxSocket();
	impl.socket(SOCK_TYPE);
    }

    // needed when the socket comes from an "accept" call
    public UNIXSocket(int fd) throws SocketException {
	this();
	// realmente necesitamos un objeto UNIXSocket con el fd des socket
	// creado por accept por lo que primero cerramos el creado y después
	// modificamos el socket contenido.
	// este trozo de código no me gusta en absoluto pero por el momento
	// así se queda.
	this.close();
	impl.setFileDescriptor(fd);
    }

    public final void connect(UNIXAddress addr) 
	throws SocketException, 
	       IllegalArgumentException {
	
	impl.connect(addr);
    }

    public final void send(byte[] msg) 
	throws IOException {

	impl.send(msg);
    }

    public final int receive(byte[] msg) 
	throws IOException {

	return impl.receive(msg);
    }

    public final OutputStream getOutputStream() {
	return new UNIXSocketOutputStream(this);
    }

    public final InputStream getInputStream() {
	return new UNIXSocketInputStream(this);
    }

    public final void close() throws SocketException {
	impl.close();
    }

}
