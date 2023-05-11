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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

/**
 * @implementation
 * It is necessary to have a finalize method because, although its system
 * resources are held by an internal {@link UnixSocketImpl}, closing an
 * object of this class involves notifying its parent {@link UnixServerSocket}.
 */
public class UnixSocket
    extends AbstractSelectableChannel
    implements Closable {

    protected final UnixServerSocket parent;
    protected final UnixSocketImpl impl;
    protected final InputStream is;
    protected final OutputStream os;

    /**
     * Creates an unconnected socket.
     */
    public UnixSocket()
    throws SocketException {
        this(null, UnixSocketImpl.usingStream());
    }
    
    public UnixSocket(final UnixSocketAddress address)
    throws SocketException {
        this();
        connect(address);
    }
    
    protected UnixSocket(final UnixServerSocket parent,
                         final UnixSocketImpl impl)
    throws SocketException {
        this.parent = parent;
        this.impl = impl;
        this.is = new SocketInputStream(this, this.impl);
        this.os = new SocketOutputStream(this, this.impl);
    }
    
    public int getFD() {
        return impl.getFD();
    }

    //----------------------------------------------------------------------
    public void bind(final UnixSocketAddress address)
    throws IOException {
        impl.bind(address);
        address.writeAttributes();
    }
    
    public void connect(final UnixSocketAddress address)
    throws SocketException {
        impl.connect(address);
    }

    
    //----------------------------------------------------------------------
    public boolean isConnected() {
        return impl.isConnected();
    }
    
    public boolean isBound() {
        return impl.isBound();
    }
    
    public boolean isClosed() {
        return impl.isClosed();
    }
    
    public SocketAddress getRemoteSocketAddress() {
        return impl.getRemoteSocketAddress();
    }
    
    public SocketAddress getLocalSocketAddress() {
        return impl.getLocalSocketAddress();
    }
    
    public InputStream getInputStream()
    throws IOException {
        return is;
    }
    
    public OutputStream getOutputStream()
    throws IOException {
        return os;
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
    
    public void setReceiveBufferSize(final int size)
    throws SocketException {
        impl.setReceiveBufferSize(size);
    }
    
    public int getReceiveBufferSize()
    throws SocketException {
        return impl.getReceiveBufferSize();
    }
    
    public void setReuseAddress(final boolean on)
    throws SocketException {
        impl.setReuseAddress(on);
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
    
    public void setSoTimeout(final long timeout)
    throws SocketException {
        impl.setSoTimeout(timeout);
    }
    
    public long getSoTimeout()
    throws SocketException {
        return impl.getSoTimeout();
    }
    
    
    //----------------------------------------------------------------------
    public void shutdownInput()
    throws IOException {
        impl.shutdown(true);
    }
    
    public void shutdownOutput()
    throws IOException {
        impl.shutdown(false);
    }
    
    public void close()
    throws IOException {
        impl.close();
        if (parent != null) parent.removeChild(this);
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
