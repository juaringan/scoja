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
package org.scoja.trans.lc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ByteChannel;
import java.nio.channels.Selector;

import org.scoja.cc.lang.Exceptions;
import org.scoja.cc.io.Streams;

import org.scoja.trans.Errors;
import org.scoja.trans.StallException;
import org.scoja.trans.ConnectState;
import org.scoja.trans.RemoteInfo;
import org.scoja.trans.TransportLine;
import org.scoja.trans.IStream;
import org.scoja.trans.OStream;
import org.scoja.trans.SelectionHandler;

/**
 * This is a proxy for {@link TransportLine} that takes care of state.
 * Simplied tasks:
 * <ul>
 * <li>The proxied TransportLine doesn't need to implement several methods
 *   (those implemented in {@link NoLCLine}),
 * <li>The proxied TransportLine, IStream and OStream don't need to check
 *   that methods are called in the correct state.
 * <li>The proxied TransportLine doesn't neet to cache the result of
 *   {@link #layers}, {@link #remote}, {@link #input} or {@link @output}.
 * </ul>
 */
public class LCLine<C> implements TransportLine<C> {
    
    protected final Object lock;
    protected final TransportLine<C> base;
    protected String layers;
    protected RemoteInfo remote;
    protected ConnectState state;
    protected LCIStream in;
    protected LCOStream out;
    protected LCInputStream is;
    protected LCOutputStream os;
    protected LCByteChannel bc;
    
    public LCLine(final TransportLine<C> base) {
        this(base, ConnectState.UNCONNECTED);
    }
    
    public LCLine(final TransportLine<C> base, final ConnectState state) {
        this.lock = new Object();
        this.base = base;
        this.layers = null;
        this.remote = null;
        this.state = state;
        this.in = null; this.out = null;
        this.is = null; this.os = null;
        this.bc = null;
    }
    
    public String layers() {
        if (layers == null) layers = base.layers();
        return layers;
    }
    
    public C configuration()
    throws IOException {
        return base.configuration();
    }
    
    public boolean isBlocking() { return base.isBlocking(); }
    
    public ConnectState connectState() {
        synchronized (lock) { return state; }
    }
    
    public ConnectState connect()
    throws IOException {
        synchronized (lock) {
            if (ConnectState.CONNECTING.compareTo(state) < 0) return state;
        }
        final ConnectState newState;
        try {
            newState = connect0();
        } catch (Throwable e) {
            disconnect(e);
            throw asIO(e);
        }
        updateState(newState);
        return newState;
    }
    
    public RemoteInfo remote() {
        if (remote == null) remote = base.remote();
        return remote;
    }
    
    public void close()
    throws IOException {
        synchronized (lock) { closeL(); }
    }
    
    public IStream input()
    throws IOException {
        synchronized (lock) {
            if (in == null) try {
                in = new LCIStream(input0L());
            } catch (Throwable e) {
                disconnectL(e);
                throw asIO(e);
            }
            return in;
        }
    }
    
    public OStream output()
    throws IOException {
        synchronized (lock) {
            if (out == null) try {
                out = new LCOStream(output0L());
            } catch (Throwable e) {
                disconnectL(e);
                throw asIO(e);
            }
            return out;
        }
    }
    
    public InputStream inputStream()
    throws IOException {
        input();
        synchronized (lock) {
            if (is == null) is = new LCInputStream();
            return is;
        }
    }
    
    public OutputStream outputStream()
    throws IOException {
        output();
        synchronized (lock) {
            if (os == null) os = new LCOutputStream();
            return os;
        }
    }
    
    public ByteChannel channel()
    throws IOException {
        input(); output();
        synchronized (lock) {
            if (bc == null) bc = new LCByteChannel();
            return bc;
        }
    }

    public SelectionHandler register(final SelectionHandler handler) {
        handler.addSelectable(this);
        return base.register(handler);
    }
        
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        synchronized (lock) {
            sb.append("LCLine[")
                .append("state: ").append(state)
                .append(", base: ").append(base)
                .append("]");
        }
        return sb.toString();
    }

    //----------------------------------------------------------------------
    protected void disconnect(final Throwable e) {
        synchronized (lock) { disconnectL(e); }
    }
    
    protected void disconnectL(final Throwable e) {
        updateStateL(ConnectState.DISCONNECTED);
    }
        
    protected void updateState(final ConnectState newState) {
        synchronized (lock) { updateStateL(newState); }
    }
    
    protected void updateStateL(final ConnectState newState) {
        //Uncomment the following 2 lines to see when a state change occurs.
        //System.err.println("STATE: " + newState);
        //new Exception().printStackTrace(System.err);
        if (state.compareTo(newState) <= 0) {
            state = newState;
        } else if (state == ConnectState.CLOSED) {
            Errors.closed(this);
        }
    }
    
    protected static IOException asIO(final Throwable e) {
        return Exceptions.uncheckedOr(IOException.class, e);
    }
    
    protected int impossibleInputL(final ConnectState state)
    throws IOException {
        switch (state) {
        case CONNECTING: return 0;
        case DISCONNECTED: return -1;
        case UNCONNECTED: throw Errors.unconnected(this);
        default: throw new IllegalStateException(
            "Should no be called when on " + state);
        }
    }
    
    protected int impossibleOutputL(final ConnectState state)
    throws IOException {
        switch (state) {
        case CONNECTING: return 0;
        case DISCONNECTED:
        case UNCONNECTED: throw Errors.unconnected(this);
        default: throw new IllegalStateException(
            "Should no be called when on " + state);
        }
    }
    
    
    //----------------------------------------------------------------------
    // Calls to the proxied Line1

    protected ConnectState connect0()
    throws IOException {
        return base.connect();
    }

    protected void closeL()
    throws IOException {
        updateStateL(ConnectState.CLOSED);
        base.close();
    }
    
    protected IStream input0L()
    throws IOException {
        return base.input();
    }
    
    protected OStream output0L()
    throws IOException {
        return base.output();
    }

    
    //======================================================================
    public class LCIStream extends IStream.Proxy {
    
        public LCIStream(final IStream base) {
            super(base);
        }
        
        public int available()
        throws IOException {
            final ConnectState state = connect();
            if (state != ConnectState.CONNECTED)
                return impossibleInputL(state);
            try {
                return super.available();
            } catch (Throwable e) {
                disconnect(e);
                throw asIO(e);
            }
        }
    
        public int read(final byte[] bs, final int off, final int len)
        throws IOException {
            final ConnectState state = connect();
            if (state != ConnectState.CONNECTED) 
                return impossibleInputL(state);
            try {
                return super.read(bs, off, len);
            } catch (Throwable e) {
                disconnect(e);
                throw asIO(e);
            }
        }
    
        public int read(final ByteBuffer bs)
        throws IOException {
            final ConnectState state = connect();
            if (state != ConnectState.CONNECTED) 
                return impossibleInputL(state);
            try {
                return super.read(bs);
            } catch (Throwable e) {
                disconnect(e);
                throw asIO(e);
            }
        }
    }    
    
    
    //======================================================================
    public class LCOStream extends OStream.Proxy {
        public LCOStream(final OStream base) {
            super(base);
        }
    
        public int write(final byte[] bs, final int off, final int len)
        throws IOException {
            final ConnectState state = connect();
            if (state != ConnectState.CONNECTED) 
                return impossibleOutputL(state);
            try {
                return super.write(bs, off, len);
            } catch (Throwable e) {
                disconnect(e);
                throw asIO(e);
            }
        }
    
        public int write(final ByteBuffer bs)
        throws IOException {
            final ConnectState state = connect();
            if (state != ConnectState.CONNECTED) 
                return impossibleOutputL(state);
            try {
                return super.write(bs);
            } catch (Throwable e) {
                disconnect(e);
                throw asIO(e);
            }
        }
    
        public int flush()
        throws IOException {
            final ConnectState state = connect();
            if (state != ConnectState.CONNECTED) 
                return impossibleOutputL(state);
            try {
                return super.flush();
            } catch (Throwable e) {
                disconnect(e);
                throw asIO(e);
            }
        }
    }
    
    
    //======================================================================
    public class LCInputStream extends InputStream {
    
        public int read()
        throws IOException {
            final byte[] bs = new byte[1];
            final int n = in.read(bs, 0, 1);
            return (n <= 0) ? -1 : (bs[0] & 0xFF);
        }
        
        public int read(final byte[] bs)
        throws IOException {
            return in.read(bs, 0, bs.length);
        }
        
        public int read(final byte[] bs, final int off, final int len)
        throws IOException {
            return in.read(bs, off, len);
        }
        
        public long skip(final long n)
        throws IOException {
            return Streams.skipWithReads(this, n);
        }
        
        public int available()
        throws IOException {
            return in.available();
        }
        
        public void close()
        throws IOException {
            LCLine.this.close();
        }
        
        public boolean markSupported() { return false; }
        
        public void mark(final int readlimit) {}
        
        public void reset()
        throws IOException {
            throw new IOException("Marks not supported");
        }
    }
    
    
    //======================================================================
    public class LCOutputStream extends OutputStream {
    
        public void write(final int n)
        throws IOException {
            write(new byte[] {(byte)n});
        }
        
        public void write(final byte[] bs)
        throws IOException {
            write(bs, 0, bs.length);
        }
        
        public void write(final byte[] bs, final int off, final int len)
        throws IOException {
            final int n = out.write(bs, off, len);
            if (n < len) {
                throw (n < 0) ? new ClosedChannelException()
                    : new StallException(len, n);
            }
        }
        
        public void flush()
        throws IOException {
            out.flush();
        }
        
        public void close()
        throws IOException {
            LCLine.this.close();
        }
    }
    
    
    //======================================================================
    public class LCByteChannel implements ByteChannel {
        
        public boolean isOpen() {
            return LCLine.this.connectState().open();
        }
        
        public void close()
        throws IOException {
            LCLine.this.close();
        }
        
        public int read(final ByteBuffer bb)
        throws IOException {
            return in.read(bb);
        }
        
        public int write(final ByteBuffer bb)
        throws IOException {
            return out.write(bb);
        }
    }
}
