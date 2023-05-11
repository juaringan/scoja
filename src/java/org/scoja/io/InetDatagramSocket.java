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
import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * This class has no use because Java already provides Internet Datagram
 * Sockets.
 * It has been implementent only for pleasure and completeness.
 */
public class InetDatagramSocket
    extends AbstractSelectableChannel
    implements Closable {

    protected final InetSocketImpl impl;

    public InetDatagramSocket() 
        throws SocketException {
        this.impl = InetSocketImpl.usingDatagram();
    }
    
    public InetDatagramSocket(final InetSocketAddress address)
        throws SocketException {
        this();
        bind(address);
    }
    
    public int getFD() {
        return impl.getFD();
    }
    
    
    //----------------------------------------------------------------------
    public void bind(final InetSocketAddress address)
    throws SocketException {
        impl.bind(address);
    }
    
    public void connect(final InetSocketAddress address)
    throws SocketException {
        impl.connect(address);
    }
    
    public void connect(final InetAddress address, final int port)
    throws SocketException {
        connect(new InetSocketAddress(address, port));
    }
    
    public void disconnect()
    throws SocketException {
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
    
    public InetAddress getInetAddress() {
        return ((InetSocketAddress)getRemoteSocketAddress()).getAddress();
    }
    
    public int getPort() {
        return ((InetSocketAddress)getRemoteSocketAddress()).getPort();
    }
    
    public SocketAddress getLocalSocketAddress() {
        return impl.getLocalSocketAddress();
    }
    
    public InetAddress getLocalAddress() {
        return ((InetSocketAddress)getLocalSocketAddress()).getAddress();
    }
    
    public int getLocalPort() {
        return ((InetSocketAddress)getLocalSocketAddress()).getPort();
    }
    
    
    //----------------------------------------------------------------------
    public void setSoTimeout(final int timeout)
    throws SocketException {
        impl.setSoTimeout(timeout);
    }
    
    public long getSoTimeout()
    throws SocketException {
        return impl.getSoTimeout();
    }
    
    public void setSendBufferSize(final int size)
    throws SocketException {
        impl.setSendBufferSize(size);
    }
    
    public int getSendBufferSize()
    throws SocketException {
        return impl.getSendBufferSize();
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
    
    public void setBroadcast(final boolean on)
    throws SocketException {
        impl.setBroadcast(on);
    }
    
    public boolean getBroadcast()
    throws SocketException {
        return impl.getBroadcast();
    }
    
    
    //----------------------------------------------------------------------
    public void send(final DatagramPacket p)
    throws IOException {
        impl.send(p);
    }
    
    public void receive(final DatagramPacket p)
    throws IOException {
        impl.receive(p);
    }
    
    public void close()
    throws IOException {
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
