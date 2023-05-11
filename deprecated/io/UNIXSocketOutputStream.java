
package org.scoja.util;

import java.io.OutputStream;
import java.io.IOException;


/**
 * A UNIXSocketOutputStream is a stream to write to a <code>UNIXSocket</code>
 *
 * <p><code>UNIXSocketOutputStream</code> is meant for writing streams of raw bytes
 *
 * @see     java.io.OutputStream
 * @see     org.scoja.UNIXSocketInputStream
 */
public final class UNIXSocketOutputStream extends OutputStream {

    private UNIXSocket socket;

    public UNIXSocketOutputStream(UNIXSocket us) {
	this.socket = us;
    }



    /**
     * Writes the specified byte to this UNIXSocket output stream. Implements 
     * the <code>write</code> method of <code>OutputStream</code>.
     *
     * @param      b   the byte to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(int b) throws IOException {
	// im not sure what to do here.

	byte bb = (byte) b;
	byte[] arr = {bb};

	socket.send(arr);
    }


    /**
     * Writes <code>b.length</code> bytes from the specified byte array 
     * to this UNIXSocket output stream. 
     *
     * @param      b   the data.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(byte[] b) throws IOException {
	socket.send(b);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array 
     * starting at offset <code>off</code> to this UNIXSocket output stream. 
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(byte b[], int off, int len) throws IOException {

	if (len <= 0 || off < 0 || off+len > b.length) {

	    if (len == 0) return;

	    throw new ArrayIndexOutOfBoundsException();
	}

	byte[] msg = new byte[len];
	for (int k=off; k<len; k++)
	    msg[k] = b[k];

	socket.send(msg);

    }


    /**
     * Closes this UNIXSocket output stream and releases any system resources 
     * associated with this stream. This UNIXSocket output stream may no longer 
     * be used for writing bytes. 
     *
     * <p> If this stream has an associated channel then the channel is closed
     * as well.
     *
     * @exception  IOException  if an I/O error occurs.
     *
     * @revised 1.4
     * @spec JSR-51
     */
    public void close() throws IOException {
	this.flush();
	this.socket.close();
    }

}
