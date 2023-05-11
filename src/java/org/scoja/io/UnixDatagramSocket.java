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

/**
 * @implementation
 * No need to add a finalize method because all system resources are held
 * by an internal {@link UnixSocketImpl}.
 */
public class UnixDatagramSocket
    extends AbstractSelectableChannel {

    protected final UnixSocketImpl impl;

    /**
     * Builds a Unix domain datagram socket
     * that is neither bound nor connected.
     * It can be used only to send messages with an explicit target.
     */
    public UnixDatagramSocket() 
    throws SocketException {
        this.impl = UnixSocketImpl.usingDatagram();
    }
    
    public UnixDatagramSocket(final UnixSocketAddress address)
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
    }
    
    public void connect(final UnixSocketAddress address)
    throws SocketException {
        impl.connect(address);
    }
    
    public void disconnect() {
        impl.disconnect();
    }

    
    //----------------------------------------------------------------------
    public boolean isBound() {
        return impl.isBound();
    }
    
    public boolean isConnected() {
        return impl.isConnected();
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
    
    
    //----------------------------------------------------------------------
    public void setDebug(final boolean on)
    throws SocketException {
        impl.setDebug(on);
    }
    
    public boolean getDebug()
    throws SocketException {
        return impl.getDebug();
    }
    
    public void setSoTimeout(final long timeout)
    throws SocketException {
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

    public void setSendBufferSize(final int size)
    throws SocketException {
        impl.setSendBufferSize(size);
    }
    
    public int getSendBufferSize()
    throws SocketException {
        return impl.getSendBufferSize();
    }
    
    public void setReuseAddress(final boolean reuse)
    throws SocketException {
        impl.setReuseAddress(reuse);
    }
    
    public boolean getReuseAddress()
    throws SocketException {
        return impl.getReuseAddress();
    }
    

    //----------------------------------------------------------------------
    public void send(final GenericDatagramPacket p)
    throws IOException {
        impl.send(p);
    }
    
    public void receive(final GenericDatagramPacket p)
    throws IOException {
        impl.receive(p);
    }
    
    public void close()
    throws SocketException {
        //impl.unlink();
        impl.close();
    }
    
    
    //======================================================================
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName()).append('[');
        impl.getAttributes(sb);
        sb.append(']');
        return sb.toString();
    }
}
