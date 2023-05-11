/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2010  Mario Martínez
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
package org.scoja.trans;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.scoja.cc.io.Buffers;

/**
 * A minimized InputStream.
 * java.io.InputStream is good enough for Transport purposes.
 * But he have defined this interface so that OStream have a couple.
 * 
 * <b>Stall</b>
 * The value returned from {@link available} and {@link read} has the
 * following meaning:
 *   a <tt>-1</tt> is the end of stream,
 *   a <tt>0</tt> is an stall,
 *   a positive value is real data.
 */
public interface IStream {

    public int available()
    throws IOException;
    
    public int read(byte[] bs, int off, int len)
    throws IOException;
    
    public int read(ByteBuffer bs)
    throws IOException;
    
    
    //======================================================================
    public static abstract class Defaults implements IStream {
        public static int readWithBuffer(final IStream is,
                final byte[] bs, final int off, final int len)
        throws IOException {
            return is.read(ByteBuffer.wrap(bs, off, len));
        }
        
        public static int readWithArray(final IStream is, final ByteBuffer bb)
        throws IOException {
            if (bb.hasArray()) {
                final int r = is.read(bb.array(), 
                        bb.arrayOffset() + bb.position(), bb.remaining());
                if (r > 0) Buffers.skip(bb, r);
                return r;
            } else {
                int total = 0, r;
                final byte[] bs = new byte[Math.min(1024, bb.remaining())];
                for (;;) {
                    final int n = Math.min(bb.remaining(), bs.length);
                    r = is.read(bs, 0, n);
                    if (r <= 0) break;
                    bb.put(bs, 0, r);
                    if (is.available() <= 0) break;
                }
                return (total > 0 || r >= 0) ? total : -1;
            }
        }
        
        public int read(final byte[] bs, final int off, final int len)
        throws IOException {
            return readWithBuffer(this, bs, off, len);
        }
        
        public int read(final ByteBuffer bs)
        throws IOException {
            return readWithArray(this, bs);
        }
    }

    
    //======================================================================
    public static class Proxy implements IStream {
        protected final IStream base;
        
        public Proxy(final IStream base) {
            this.base = base;
        }
        
        public int available()
        throws IOException {
            return base.available();
        }
    
        public int read(final byte[] bs, final int off, final int len)
        throws IOException {
            return base.read(bs, off, len);
        }
    
        public int read(final ByteBuffer bs)
        throws IOException {
            return base.read(bs);
        }
    }
        
    
    //======================================================================
    public static class FromInputStream implements IStream {
        protected final InputStream in;
        
        public FromInputStream(final InputStream in){
            this.in = in;
        }
        
        public int available()
        throws IOException {
            return in.available();
        }
    
        public int read(final byte[] bs, final int off, final int len)
        throws IOException {
            return in.read(bs, off, len);
        }
        
        public int read(final ByteBuffer bs)
        throws IOException {
            return Buffers.put(bs, in);
        }
    }
    
    
    //======================================================================
    /**
     * It works with any blocking mode.
     * If the SocketChannel is blocking, its Socket's InputStream will be
     * used for {@link #available} and {@link #read(byte[],int,int)}.
     * If the SocketChannel is not blocking,
     * there is no risk in {@link #available} saying that there is always data,
     * and {@link #read(byte[],int,int)} will wrap the byte[] in a ByteBuffer.
     */
    public static class FromConnectedSocketChannel implements IStream {
        protected final SocketChannel channel;
        protected boolean eos;
        
        public FromConnectedSocketChannel(final SocketChannel channel) {
            this.channel = channel;
            this.eos = false;
        }
        
        public int available()
        throws IOException {
            return (eos || !channel.isOpen()) ? -1
                : !channel.isBlocking() ? 1
                : channel.socket().getInputStream().available();
        }
        
        public int read(final byte[] bs, final int off, final int len)
        throws IOException {
            final int n;
            if (channel.isBlocking()) {
                n = channel.socket().getInputStream().read(bs, off, len);
            } else {
                final ByteBuffer w = ByteBuffer.wrap(bs, off, len);
                n = channel.read(w);
            }
            eos = n < 0;
            return n;
        }
        
        public int read(final ByteBuffer bs)
        throws IOException {
            final int n = channel.read(bs);
            eos = n < 0;
            return n;
        }
    }
    
    
    //======================================================================
    /**
     * A maybe-connecting SocketChannel must be a non-blocking channel.
     * So, it is not legal to read from its Socket.
     */
    public static class FromConnectingSocketChannel implements IStream {
        protected final SocketChannel channel;
        protected boolean eos;
        
        public FromConnectingSocketChannel(final SocketChannel channel) {
            this.channel = channel;
            this.eos = false;
        }
        
        public int available()
        throws IOException {
            return eos ? -1
                : channel.isConnected() ? 1
                : channel.isConnectionPending() ? 0
                : -1;
        }
        
        public int read(final byte[] bs, final int off, final int len)
        throws IOException {
            return read(ByteBuffer.wrap(bs, off, len));
        }
        
        public int read(final ByteBuffer bs)
        throws IOException {
            if (channel.isConnectionPending()) {
                if (!channel.finishConnect()) return 0;
            }
            final int n = channel.read(bs);
            eos = eos || n < 0;
            return n;
        }            
    }
    
    
    //======================================================================
    public static class ByteArray implements IStream {
    
        protected final byte[] data;
        protected int next;
        protected final int last;
        
        public ByteArray(final byte[] data, final int off, final int len) {
            this.data = data;
            this.next = off;
            this.last = off + len;
        }
        
        public int available()
        throws IOException {
            return (next < last) ? (last-next) : -1;
        }
    
        public int read(final byte[] bs, final int off, final int len)
        throws IOException {
            final int n = Math.min(len, last-next);
            System.arraycopy(data, next, bs, off, n);
            next += n;
            return n;
        }
        
        public int read(final ByteBuffer bs)
        throws IOException {
            final int n = Math.min(bs.remaining(), last-next);
            bs.put(data, next, n);
            next += n;
            return n;
        }
    }

            
    //======================================================================
    public static class Concat implements IStream {
        protected final IStream[] iss;
        protected int current;

        public Concat(final IStream[] iss) {
            this.iss = iss;
            this.current = 0;
        }
        
        public int available()
        throws IOException {
            while (current < iss.length
                    && iss[current].available() < 0) current++;
            return (current < iss.length) ? iss[current].available() : -1;
        }
    
        public int read(final byte[] bs, final int off, final int len)
        throws IOException { 
            int r = -1;
            while (current < iss.length) {
                r = iss[current].available();
                if (r > 0) r = iss[current].read(bs, off, len);
                if (r >= 0) break;
                current++;
            }
            return r;
        }
        
        public int read(final ByteBuffer bs)
        throws IOException {
            int r = -1;
            while (current < iss.length) {
                r = iss[current].available();
                if (r > 0) r = iss[current].read(bs);
                if (r >= 0) break;
                current++;
            }
            return r;
        }
    }
}
