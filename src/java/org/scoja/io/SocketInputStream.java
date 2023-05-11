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

import java.io.InputStream;
import java.io.IOException;

/**
 * This class is a stream to read from a {@link SocketImpl}.
 * It is meant for reading streams of raw bytes.
 *
 * @see java.io.InputStream
 * @see org.scoja.io.SocketOutputStream
 */
final class SocketInputStream extends InputStream {

    protected final Object socket;
    protected final SocketImpl impl;

    protected SocketInputStream(final Object socket,
                                final SocketImpl impl) {
        this.socket = socket;
	this.impl = impl;
    }

    public int read()
    throws IOException {
        return impl.receive();
    }

    public int read(final byte b[], final int off, final int len)
    throws IOException {
	if (len <= 0 || off < 0 || off+len > b.length) {
	    if (len == 0) return 0;
	    throw new ArrayIndexOutOfBoundsException
                ("Trying to read " + len + " bytes from index " + off
                 + " into an array of length " + b.length);
	}
        
	final int received = impl.receive(b, off, len);
	return (received < 0) ? -1 : received;
    }

    public void close()
    throws IOException {
	impl.shutdown(true);
    }
    
    
    //======================================================================
    public String toString() {
        return getClass().getName() + "[" + socket + "]";
    }
}
