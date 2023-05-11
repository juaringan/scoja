/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
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
import java.io.InterruptedIOException;
 
public class CharBuffer {

    protected final char[] buffer;
    protected int init;
    protected int inUse;
    protected boolean writeEnded;
    protected boolean readEnded;

    public CharBuffer(final int size) {
        this.buffer = new char[size];
        this.init = 0;
        this.inUse = 0;
        this.writeEnded = false;
        this.readEnded = false;
    }
    
    public Reader getReader() {
        return new Reader();
    }
    
    public Writer getWriter() {
        return new Writer();
    }
    
    public synchronized void writeEnded() {
        writeEnded = true;
        notifyAll();
    }
    
    public synchronized void readEnded() {
        readEnded = true;
        notifyAll();
    }
    
    public synchronized int read()
    throws InterruptedException {
        while (inUse == 0) {
            if (writeEnded) return -1;
            else wait();
        }
        final char result = buffer[init];
        init++;
        if (init >= buffer.length) init = 0;
        notifyAll();
        return result;
    }
    
    public synchronized int read(final char[] data, 
                                 final int off, final int len)
    throws InterruptedException {
        int i = off, remain = len;
        while (remain > 0) {
            if (inUse == 0) {
                if (writeEnded || (remain != len)) break;
                else wait();
            } else {
                final int toCopy
                    = Math.min(remain, Math.min(inUse, buffer.length-init));
                System.arraycopy(buffer,init, data,i,toCopy);
                remain -= toCopy;
                i += toCopy;
                inUse -= toCopy;
                init = (init + toCopy) % buffer.length;
                notifyAll();
            }
        }
        return (remain == len && writeEnded) ? -1 : (len - remain);
    }
    
    public synchronized long skip(final long n)
    throws InterruptedException {
        long remain = n;
        while (remain > 0) {
            if (inUse == 0) {
                if (writeEnded || (remain != n)) break;
                else wait();
            } else {
                final int toSkip = (int)Math.min(
                    remain, Math.min(inUse, buffer.length-init));
                remain -= toSkip;
                inUse -= toSkip;
                init = (init + toSkip) % buffer.length;
                notifyAll();
            }
        }
        return n - remain;
    }
    
    public synchronized void write(final char b)
    throws InterruptedException, IOException {
        for (;;) {
            if (readEnded) {
                throw new IOException("Reading ended");
            }
            if (inUse != buffer.length) break;
            wait();
        }
        buffer[(init + inUse) % buffer.length] = b;
        inUse++;
        notifyAll();
    }
    
    public synchronized void write(final char[] data,
                                   final int off, final int len)
    throws InterruptedException, IOException {
        int i = off, remain = len;
        while (remain > 0) {
            if (readEnded) {
                throw new IOException("Reading ended");
            } else if (inUse == buffer.length) {
                wait();
            } else {
                final int copyTo = (init+inUse) % buffer.length;
                final int toCopy
                    = Math.min(remain, buffer.length - Math.max(inUse,copyTo));
                System.arraycopy(data,i, buffer,copyTo,toCopy);
                remain -= toCopy;
                i += toCopy;
                inUse += toCopy;
                notifyAll();
            }
        }
    }
    
    public synchronized void flush()
    throws InterruptedException, IOException {
        while (inUse > 0) {
            if (readEnded) {
                throw new IOException("Reading ended");
            } else {
                wait();
            }
        }
    }
    
    
    //======================================================================
    public class Reader
        extends java.io.Reader {
        
        public int read()
        throws IOException {
            try {
                return CharBuffer.this.read();
            } catch (InterruptedException e) {
                throw (IOException)new InterruptedIOException().initCause(e);
            }
        }
        
        public int read(final char[] cbuf)
        throws IOException {
            return read(cbuf, 0, cbuf.length);
        }
        
        public int read(final char[] cbuf, final int off, final int len)
        throws IOException {
            try {
                return CharBuffer.this.read(cbuf, off, len);
            } catch (InterruptedException e) {
                throw (IOException)new InterruptedIOException().initCause(e);
            }
        }
        
        public long skip(final long n)
        throws IOException {
            try {
                return CharBuffer.this.skip(n);
            } catch (InterruptedException e) {
                throw (IOException)new InterruptedIOException().initCause(e);
            }
        }
        
        public boolean ready()
        throws IOException {
            //FIXME
            return false;
        }
        
        public void close() {
            CharBuffer.this.readEnded();
        }
    }
    
    
    //======================================================================
    public class Writer
        extends java.io.Writer {
        
        public void write(final int c)
        throws IOException {
            try {
                CharBuffer.this.write((char)c);
            } catch (InterruptedException e) {
                throw (IOException)new InterruptedIOException().initCause(e);
            }
        }
        
        public void write(final char[] cbuf)
        throws IOException {
            write(cbuf, 0, cbuf.length);
        }
        
        public void write(final char[] cbuf, final int off, final int len)
        throws IOException {
            try {
                CharBuffer.this.write(cbuf, off, len);
            } catch (InterruptedException e) {
                throw (IOException)new InterruptedIOException().initCause(e);
            }
        }
        
        public void write(final String str)
        throws IOException {
            write(str.toCharArray());
        }
        
        public void write(final String str, final int off, final int len)
        throws IOException {
            final char[] tmp = new char[len];
            str.getChars(off, off+len, tmp, 0);
            write(tmp);
        }
        
        public void flush()
        throws IOException {
            try {
                CharBuffer.this.flush();
            } catch (InterruptedException e) {
                throw (IOException)new InterruptedIOException().initCause(e);
            }
        }
        
        public void close() {
            CharBuffer.this.writeEnded();
        }
    }
}
