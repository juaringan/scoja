/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003  Mario Martínez
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.scoja.io;

import java.io.OutputStream;
import java.io.IOException;


/**
 * This class is a stream to write to a {@link SocketImpl}.
 * It is meant for writing streams of raw bytes.
 *
 * @see java.io.OutputStream
 * @see org.scoja.io.SocketInputStream
 */
final class SocketOutputStream extends OutputStream {

    protected final Object socket;
    protected final SocketImpl impl;

    protected SocketOutputStream(final Object socket,
                                 final SocketImpl impl) {
        this.socket = socket;
	this.impl = impl;
    }

    public void write(int b)
    throws IOException {
        impl.send(b);
    }

    public void write(final byte b[], final int off, final int len)
    throws IOException {
	if (len <= 0 || off < 0 || off+len > b.length) {
	    if (len == 0) return;
	    throw new ArrayIndexOutOfBoundsException
                ("Trying to write " + len + " bytes starting at " + off
                 + " from an array of length " + b.length);
	}
	impl.send(b, off, len);
    }

    public void flush()
    throws IOException {
        impl.flush();
    }
    
    public void close()
    throws IOException {
	impl.shutdown(false);
    }
    
    
    //======================================================================
    public String toString() {
        return getClass().getName() + "[" + socket + "]";
    }
}
