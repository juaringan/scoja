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
import java.io.OutputStream;
import java.nio.channels.ByteChannel;

import org.scoja.cc.lang.Exceptions;

/**
 * A normal transport line can be used once.
 * It passed through the states in {@link ConnectState} in order, although
 * some states may be avoided; it never goes back in the state order.
 * A normal TransportLine is single-threaded;
 * the thread currently using the TransportLine is the <i>owner</i>.
 * The only exception is closing: any thread can call the {@link #close}
 * method to make all the closing stuff but also to notify the owner that
 * it should use the line no more.
 *
 * <p>
 * The pattern usage is pretty simple:
 * create, configure, connect, use its IStream and OStream, and finally close.
 * It is compulsory to call connect before using the IStream or OStream.
 * But is it not necessary that the line is in {@link ConnectState#CONNECTED}.
 * Any other state will produce
 * an stall ({@link ConnectState#CONNECTING}),
 * an "end of stream" ({@link ConnectState#DISCONNECTED})
 * or an error ({@link ConnectState#UNCONNECTED}, {@link ConnectState#CLOSED}).
 * <p>
 * A line coming from a {@link TransportService} has already started its
 * connection process, so its never in {@link ConnectState#UNCONNECTED} state.
 * <p>
 * Configuration can be requested in any state, but configuration parameters
 * will only take effect if called on {@link ConnectState#UNCONNECTED}.
 */
public interface TransportLine<C> extends Selectable {

    public String layers();

    public C configuration()
    throws IOException;

    public boolean isBlocking();
    
    public ConnectState connectState();

    public ConnectState connect()
    throws IOException;
    
    public RemoteInfo remote();
    
    public void close()
    throws IOException;
    
    public SelectionHandler register(SelectionHandler handler);
    
    public IStream input()
    throws IOException;
    
    public OStream output()
    throws IOException;

    public InputStream inputStream()
    throws IOException;
    
    public OutputStream outputStream()
    throws IOException;
    
    public ByteChannel channel()
    throws IOException;
    
    
    //======================================================================
    public static class TypeAdaptor<C> implements TransportLine<C> {
        protected final TransportLine<?> base;
        
        public TypeAdaptor(final TransportLine<?> base) {
            this.base = base;
        }
        
        public String layers() { return base.layers(); }        
        
        public C configuration() { return null; }

        public boolean isBlocking() { return base.isBlocking(); }
    
        public ConnectState connectState() { return base.connectState(); }

        public ConnectState connect()
        throws IOException {
            return base.connect();
        }
    
        public RemoteInfo remote() {
            return base.remote();
        }
    
        public void close()
        throws IOException {
            base.close();
        }
    
        public SelectionHandler register(final SelectionHandler handler) {
            handler.addSelectable(this);
            return base.register(handler);
        }
        
        public IStream input()
        throws IOException {
            return base.input();
        }
    
        public OStream output()
        throws IOException {
            return base.output();
        }
        
        public InputStream inputStream()
        throws IOException {
            return base.inputStream();
        }
    
        public OutputStream outputStream()
        throws IOException {
            return base.outputStream();
        }
    
        public ByteChannel channel()
        throws IOException {
            return base.channel();
        }
        
        public String toString() {
            return base.toString();
        }
    }
}
