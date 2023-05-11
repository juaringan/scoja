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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.SocketChannel;

import org.scoja.cc.io.Buffers;

/**
 * This is a minimized and (possible) non-blocking version of OutputStream.
 * The non-blocking aspect brings this interface to life;
 * that is, OutputStream is not an alternative.
 * java.nio.WritableByteChannel is neither an alternative:
 * it has a close, it has not a flush.
 */
public interface OStream {
    /**
     * Returns how many bytes from <tt>bs</tt> has been really written.
     */
    public int write(byte[] bs, int off, int len)
    throws IOException;
    
    public int write(ByteBuffer bs)
    throws IOException;
    
    /**
     * Returns how many bytes remains to be written.
     * It is a lower bound; but it is only 0 when all the data has been send
     * through all the transport stack.
     */
    public int flush()
    throws IOException;
    
    
    //======================================================================
    public static abstract class Defaults implements OStream {
        public static int writeWithBuffer(final OStream os,
                final byte[] bs, final int off, final int len)
        throws IOException {
            return os.write(ByteBuffer.wrap(bs, off, len));
        }
        
        public static int writeWithArray(final OStream os, final ByteBuffer bb)
        throws IOException {
            final int total = bb.remaining();
            if (bb.hasArray()) {
                os.write(
                    bb.array(), bb.arrayOffset() + bb.position(), total);
                bb.position(bb.limit());
            } else {
                final byte[] bs = new byte[Math.min(1024, bb.remaining())];
                while (bb.hasRemaining()) {
                    final int n = Math.min(bb.remaining(), bs.length);
                    bb.get(bs, 0, n);
                    os.write(bs, 0, n);
                }
            }
            return total;
        }
        
        public int write(final byte[] bs, final int off, final int len)
        throws IOException {
            return writeWithBuffer(this, bs, off, len);
        }
    
        public int write(final ByteBuffer bs)
        throws IOException {
            return writeWithArray(this, bs);
        }
    }
    
    
    //======================================================================
    public static class Proxy implements OStream {
        protected final OStream base;
        
        public Proxy(final OStream base) {
            this.base = base;
        }
        
        public int write(final byte[] bs, final int off, final int len)
        throws IOException {
            return base.write(bs, off, len);
        }
    
        public int write(final ByteBuffer bs)
        throws IOException {
            return base.write(bs);
        }
    
        public int flush()
        throws IOException {
            return base.flush();
        }
    }
    
    
    //======================================================================
    public static class FromOutputStream implements OStream {
        protected final OutputStream out;
        
        public FromOutputStream(final OutputStream out) {
            this.out = out;
        }
        
        public int write(final byte[] bs, final int off, final int len)
        throws IOException {
            out.write(bs, off, len);
            return len;
        }
        
        public int write(final ByteBuffer bs)
        throws IOException {
            return Buffers.get(bs, out);
        }
    
        public int flush()
        throws IOException {
            out.flush();
            return 0;
        }
    }
    
    
    //======================================================================
    public static class FromConnectedSocketChannel implements OStream {
        protected final SocketChannel channel;
        
        public FromConnectedSocketChannel(final SocketChannel channel) {
            this.channel = channel;
        }
    
        public int write(final byte[] bs, final int off, final int len)
        throws IOException {
            return write(ByteBuffer.wrap(bs, off, len));
        }
        
        public int write(final ByteBuffer bs)
        throws IOException {
            return channel.write(bs);
        }
    
        public int flush()
        throws IOException {
            return 0;
        }
    }
    
    
    //======================================================================
    public static class FromConnectingSocketChannel implements OStream {
        protected final SocketChannel channel;
        
        public FromConnectingSocketChannel(final SocketChannel channel) {
            this.channel = channel;
        }
    
        public int write(final byte[] bs, final int off, final int len)
        throws IOException {
            return write(ByteBuffer.wrap(bs, off, len));
        }
        
        public int write(final ByteBuffer bs)
        throws IOException {
            if (!channel.isConnectionPending()) {
                if (!channel.finishConnect()) return 0;
            }
            return channel.write(bs);
        }
    
        public int flush()
        throws IOException {
            return 0;
        }
    }
}
