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
 * The output side of a pipe.
 * Do not create object of this class directly.
 * Instead, use {@link Pipe#getSink()}.
 * This class is visible so that it selecting capabilities can be used.
 */
public class PipeOutputStream 
    extends OutputStream
    implements SelectableChannel {

    protected final Pipe pipe;

    protected PipeOutputStream(final Pipe pipe) {
	this.pipe = pipe;
    }

    
    //======================================================================
    public void write(int b)
    throws IOException {
        pipe.write(b);
    }

    public void write(final byte b[], final int off, final int len)
    throws IOException {
	pipe.write(b, off, len);
    }

    public void flush()
    throws IOException {
        //pipe.flush();
    }
    
    public void close()
    throws IOException {
	pipe.closeSink();
    }
    
    
    //======================================================================
    public int getFD() {
        return pipe.sinkFD;
    }

    public int validOps() {
        return SelectionKey.OP_WRITE;
    }
        
    public SelectionKey register(final Selector sel, final int ops) {
        final SelectionKey skey = new SelectionKey(sel, this, ops);
        sel.addKey(skey);
        return skey;
    }
    
    public SelectionKey register(final Selector sel, final int ops,
                                 final Object attribute) {
        final SelectionKey skey = register(sel, ops);
        skey.attach(attribute);
        return skey;
    }
}
