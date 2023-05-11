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

import java.io.IOException;

import org.scoja.io.posix.Posix;
import org.scoja.io.posix.PosixLike;

/**
 * This class abstract a Posix <i>pipe</i>.
 * From JDK1.4, this class has no directe use because java.nio has a portable 
 * Pipe object.
 * Nevertheless, <tt>Pipe</tt>s are this package preferred way to awake
 * threads waiting in I/O operations.
 * For instance, to wake up a thread waiting in a {@link Selector#select}
 * an internal Pipe is used: this pipe read-half is added to the set of
 * selectable descriptors, and {@link Selector#close()} writes to the
 * its write-half.
 */
public class Pipe {

    protected final PosixLike posix;
    
    protected int sourceFD;
    protected int sinkFD;
    protected final PipeInputStream source;
    protected final PipeOutputStream sink;

    public Pipe()
    throws IOException {
        this.posix = Posix.getPosix();
        
        final long fds = posix.newPipe();
        sourceFD = (int)(fds >>> 32);
        sinkFD = (int)(fds & 0x7FFFFFFF);
        this.source = new PipeInputStream(this);
        this.sink = new PipeOutputStream(this);
    }

    public PipeInputStream getSource()
    throws IOException {
        return source;
    }
    
    public PipeOutputStream getSink()
    throws IOException {
        return sink;
    }
    
    public void close()
    throws IOException {
        closeSource();
        closeSink();
    }
    
    public synchronized void closeSource()
    throws IOException {
        if (sourceFD == -1) return;
        posix.close(sourceFD);
        sourceFD = -1;
    }
    
    public synchronized void closeSink()
    throws IOException {
        if (sinkFD == -1) return;
        posix.close(sinkFD);
        sinkFD = -1;
    }
    
    protected void finalize() {
        try { close(); } catch (IOException e) {}
    }
    
    
    //======================================================================
    protected int read()
    throws IOException {
        return posix.read(sourceFD);
    }
    
    protected int read(final byte[] b, final int off, final int len)
    throws IOException {
	if (len <= 0 || off < 0 || off+len > b.length) {
	    if (len == 0) return 0;
	    throw new ArrayIndexOutOfBoundsException
                ("Try to read " + len + " bytes from index " + off
                 + " into an array of length " + b.length);
	}
	final int readed = posix.read(sourceFD, b, off, len);
	return (readed < 0) ? -1 : readed;
    }
    
    protected void write(final int b)
    throws IOException {
        posix.write(sinkFD, b);
    }
    
    protected void write(final byte[] b, final int off, final int len)
    throws IOException {
	if (len <= 0 || off < 0 || off+len > b.length) {
	    if (len == 0) return;
	    throw new ArrayIndexOutOfBoundsException
                ("Trying to write " + len + " bytes starting at " + off
                 + " from an array of length " + b.length);
	}
	posix.write(sinkFD, b, off, len);
    }
}
