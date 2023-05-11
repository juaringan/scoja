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
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * @implementation
 * It is necessary to have a finalize method because, although its system
 * resources are held by an internal {@link UnixSocketImpl}, closing an
 * object of this class involves closing all its accepted {@link UnixSocket}s.
 */
public class UnixServerSocket
    extends AbstractSelectableChannel
    implements Closable {

    private static final int INCOMING_QUEUE_LIMIT = 10;

    protected final UnixSocketImpl impl;
    protected final ClosableChildren children;

    /**
     * Creates an unbound Unix domain server socket.
     */
    public UnixServerSocket()
    throws SocketException {
        this.impl = UnixSocketImpl.usingStream();
        this.children = new ClosableChildren();
    }
    
    public UnixServerSocket(final UnixSocketAddress address) 
    throws IOException {
        this();
        bind(address);
    }
    
    public int getFD() {
        return impl.getFD();
    }
    
    
    //----------------------------------------------------------------------
    public void bind(final UnixSocketAddress address)
    throws IOException {
        impl.bind(address);
        address.writeAttributes();
        impl.listen(INCOMING_QUEUE_LIMIT);
    }
    
    
    //----------------------------------------------------------------------
    public boolean isBound() {
        return impl.isBound();
    }
    
    public boolean isClosed() {
        return impl.isClosed();
    }
    
    public SocketAddress getLocalSocketAddress() {
        return impl.getLocalSocketAddress();
    }
    

    //----------------------------------------------------------------------
    public void setDebug(final boolean on)
    throws SocketException {
        impl.setDebug(on);
    }
    
    public boolean getDebug()
    throws SocketException {
        return impl.getDebug();
    }
    
    public void setKeepAlive(final boolean on)
    throws SocketException {
        impl.setKeepAlive(on);
    }
    
    public boolean getKeepAlive()
    throws SocketException {
        return impl.getKeepAlive();
    }
    
    public void setOOBInline(final boolean on)
    throws SocketException {
        impl.setOOBInline(on);
    }
    
    public boolean getOOBInline()
    throws SocketException {
        return impl.getOOBInline();
    }
    
    public void setSoTimeout(final long timeout)
    throws SocketException {
        //CHECK: may accept is no affected by read timeouts.
        impl.setSoTimeout(timeout);
    }
    
    public long getSoTimeout()
    throws SocketException {
        return impl.getSoTimeout();
    }

    public void setReceiveBufferSize(final int size)
    throws SocketException {
        impl.setReceiveBufferSize(size);
    }
    
    public int getReceiveBufferSize()
    throws SocketException {
        return impl.getReceiveBufferSize();
    }
    
    public void setReuseAddress(final boolean reuse)
    throws SocketException {
        impl.setReuseAddress(reuse);
    }
    
    public boolean getReuseAddress()
    throws SocketException {
        return impl.getReuseAddress();
    }

    public void setSendBufferSize(final int size)
    throws SocketException {
        impl.setSendBufferSize(size);
    }
    
    public int getSendBufferSize()
    throws SocketException {
        return impl.getSendBufferSize();
    }
    
    public void setSoLinger(final boolean on, final int linger)
    throws SocketException {
        impl.setSoLinger(on, linger);
    }
    
    public int getSoLinger()
    throws SocketException {
        return impl.getSoLinger();
    }
    
    
    //----------------------------------------------------------------------
    public UnixSocket accept()
    throws IOException, SocketTimeoutException {
        final UnixSocket socket = impl.accept(this);
        children.addChild(socket);
        return socket;
    }
    
    public void close()
    throws IOException {
        SocketException firstException = null;
        try {
            impl.close();
        } catch (SocketException e) {
            if (firstException == null) firstException = e;
        }
        try {
            children.closeAll();
        } catch (SocketException e) {
            if (firstException == null) firstException = e;
        }
        if (firstException != null) throw firstException;
    }
    
    
    //======================================================================
    protected void removeChild(final UnixSocket child) {
        children.removeChild(child);
    }

        
    //======================================================================
    protected void finalize() {
        try { close(); } catch (IOException e) {}
    }
    
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName()).append('[');
        impl.getAttributes(sb);
        sb.append(']');
        return sb.toString();
    }
}
