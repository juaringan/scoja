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
import java.net.DatagramPacket;
import java.net.SocketException;

import org.scoja.io.posix.Posix;
import org.scoja.io.posix.PosixLike;
import org.scoja.io.posix.InetSocketDescription;

class InetSocketImpl
    extends SocketImpl {

    protected InetSocketAddress localAddress;
    protected InetSocketAddress remoteAddress;

    protected InetSocketImpl() {
        this(-1, null, null);
    }
    
    protected InetSocketImpl(final int fd,
                             final InetSocketAddress localAddress,
                             final InetSocketAddress remoteAddress) {
        super(fd);
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public static InetSocketImpl usingDatagram()
    throws SocketException {
        final InetSocketImpl socket = new InetSocketImpl();
        socket.openDatagram();
        return socket;
    }
    
    public static InetSocketImpl usingStream()
    throws SocketException {
        final InetSocketImpl socket = new InetSocketImpl();
        socket.openStream();
        return socket;
    }
    
    protected void openDatagram()
    throws SocketException {
        this.fd = posix.newInetDatagram();
    }
    
    protected void openStream()
    throws SocketException {
        this.fd = posix.newInetStream();
    }
    
    
    //----------------------------------------------------------------------
    private int ip(final InetSocketAddress address) {
        return ip(address.getAddress().getAddress());
    }
    private int ip(final byte[] ipbs) {
        return ((ipbs[0] & 0xFF) << 24)
            | ((ipbs[1] & 0xFF) << 16)
            | ((ipbs[2] & 0xFF) << 8)
            | ((ipbs[3] & 0xFF) << 0);
    }
    
    public void bind(final InetSocketAddress address)
    throws SocketException {
        posix.bind(fd, ip(address), address.getPort());
        this.localAddress = address;
    }
    
    public void connect(final InetSocketAddress address)
    throws SocketException {
        posix.connect(fd, ip(address), address.getPort());
        this.remoteAddress = address;
    }
    
    public void disconnect() {
        //FIXME: What to do?
        this.remoteAddress = null;
    }
    
    public InetSocket accept(final InetServerSocket acceptor)
    throws SocketException {
        closing.checkClosed(false);
        final InetSocketDescription childDesc = posix.acceptInet(fd);
        if (childDesc == null) return null;
        closing.checkClosed(false);
        return new InetSocket(
            acceptor,
            new InetSocketImpl(
                childDesc.getFD(),
                localAddress,
                childDesc.getClientAddress()));
    }
    
    
    //----------------------------------------------------------------------
    public boolean isBound() {
        return localAddress != null;
    }
    
    public boolean isConnected() {
        return remoteAddress != null;
    }
    
    public SocketAddress getRemoteSocketAddress() {
        return remoteAddress;
    }
    
    public SocketAddress getLocalSocketAddress() {
        return localAddress;
    }
    
    
    //----------------------------------------------------------------------
    public void send(final DatagramPacket p)
    throws IOException {
        closing.checkClosed(false);
        posix.send(fd, p.getData(), p.getOffset(), p.getLength());
    }
    
    public void receive(final DatagramPacket p)
    throws IOException {
        closing.checkClosed(false);
        posix.receiveFrom(fd, p);
    }
    
    public void close0()
    throws SocketException {
        super.close0();
        remoteAddress = localAddress = null;
    }
}
