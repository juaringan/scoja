
package org.scoja.util;

import java.io.InputStream;
import java.io.IOException;

/**
 * A UNIXSocketInputStream is a stream to read from a <code>UNIXSocket</code>
 *
 * <p><code>UNIXSocketInputStream</code> is meant for reading streams of raw bytes
 *
 * @see     java.io.IntputStream
 * @see     org.scoja.UNIXSocketInputStream
 */
public final class UNIXSocketInputStream extends InputStream {

    private UNIXSocket socket;

    //private boolean isClosed = false;


    public UNIXSocketInputStream(UNIXSocket us) {
	this.socket = us;
    }


    /**
     * Reads the specified byte from this UNIXSocket input stream. Implements 
     * the <code>read</code> method of <code>InputStream</code>.
     *
     * @return     the next byte of data, or <code>-1</code> if the 
     *             stream has been closed.
     * @exception  IOException  if an I/O error occurs.
     */
    public int  read() throws IOException {

	byte[] arr = new byte[1];
	
	if (this.read(arr,0,1) == -1) return -1;

	return arr[0];
    }


    /**
     * Reads up to <code>b.length</code> bytes of data from this socket input
     * stream into an array of bytes. This method blocks until some input
     * is available.
     *
     * @param      b   the buffer into which the data is read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because 
     *             the socket stream has been closed.
     * @exception  IOException  if an I/O error occurs.
     */
    public int read(byte b[]) throws IOException {

	return this.read(b,0,b.length);
    }


    /**
     * Reads up to <code>len</code> bytes of data from this socket input stream
     * into an array of bytes. This method blocks until some input is
     * available.
     *
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset of the data.
     * @param      len   the maximum number of bytes read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because the 
     *             stream socket has been closed.
     * @exception  IOException  if an I/O error occurs.
     */
    public int read(byte b[], int off, int len) throws IOException {

	if (len <= 0 || off < 0 || off+len > b.length) {
	    if (len == 0) return 0;

	    throw new ArrayIndexOutOfBoundsException();
	}

	byte[] aux = new byte[len];
	int res = this.socket.receive(aux);


	// write up to "res" not up to "len" because maybe
	// the native socket driver could _not_ read enough bytes.
	for (int k=0; k<res; k++)
	    b[off+k] = aux[k];


	// indicates a closed end-point socket
	if (res <= 0) {
	    //isClosed = true;
	    this.close();
	    return -1;
	}

	return res;
    }


    /**
     * Closes this unix socket input stream and releases any system resources
     * associated with the stream.
     *
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close() throws IOException {
	this.socket.close();
    }


}
