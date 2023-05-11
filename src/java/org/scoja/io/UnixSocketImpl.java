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

import org.scoja.io.posix.Posix;
import org.scoja.io.posix.PosixLike;
import org.scoja.io.posix.UnixSocketDescription;

/**
 * This class is the real Unix domain socket implementation.
 * All Unix sockets, both datagram and stream, use this class almost
 * directly to implement theirs operations.
 * So, this class has operations for datagram socket and for stream
 * socket; it is the responsability of the wrapping class to use only
 * those operations that make sense for its socket kind.
 * 
 * <p><b>Throwing {@link IOException} versus {@link SocketException}</b>.
 * Some methods declare throwing {@link SocketException}
 * but others declare the less especific {@link IOException}.
 * This difference makes sense because some operations are related to
 * I/O (like {@link #send(byte[],int,int)}
 * or {@link #receive(byte[],int,int)}) while others are dedicated to
 * socket building (link constructors).
 * Closed errors are a tricky deal.
 * If we use {@link java.nio.channels.ClosedChannelException} that
 * inherits directly from {@link IOException}, then
 * methods like {@link #accept} should throw {@link IOException}.
 * To avoid this extrange throws clauses, and not to relay on
 * java.nio, we use {@link java.io.ClosedSocketException} to signal closed
 * errors. This class inherits directly from {@link SocketException}.
 *
 * @bugs
 * Currently closing a socket while other threads are using it can
 * result in operation with other socket, file, ...
 * Let us see how this happen.
 * Suppose the socket S has file descriptor N.
 * <ol>
 * <li>Thread A calls <code>receive</code>, gets the file descriptor N
 *     from {@link #fd} and pass it to <code>posix.receive</code>.
 * <li>Thread B calls <code>close</code>. This calls
 *     <code>close0</code> that frees the file descriptor N so that it
 *     can be used for another purpose.
 *     Then stores <code>-1</code> in {@link #fd}.
 * <li>Thread C opens some unix channel that allocates the file
 *     descriptor N.
 * <li>Thread A continues its execution of <code>posix.receive</code> where
 *     descriptor N is used, legally but retriving data from another
 *     channel.
 * </ol>
 * <p>
 * To avoid this problem a counter with the file descriptor usage is
 * needed.
 * When a socket is closed, if the counter is 0 the file descriptor is
 * closed.
 * Otherwise a flag is set.
 * Those threads wanting to increment the counter will get a
 * SocketClosedException. 
 * The thread that makes the counter 0 when the flag is set has to
 * close the file descriptor.
 */
class UnixSocketImpl 
    extends SocketImpl {

    protected UnixSocketAddress localAddress;
    protected UnixSocketAddress remoteAddress;
    
    protected UnixSocketImpl() {
        this(-1, null, null);
    }
    
    protected UnixSocketImpl(final int fd,
                             final UnixSocketAddress localAddress,
                             final UnixSocketAddress remoteAddress) {
        super(fd);
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public static UnixSocketImpl usingDatagram()
    throws SocketException {
        final UnixSocketImpl socket = new UnixSocketImpl();
        socket.openDatagram();
        return socket;
    }
    
    public static UnixSocketImpl usingStream()
    throws SocketException {
        final UnixSocketImpl socket = new UnixSocketImpl();
        socket.openStream();
        return socket;
    }
    
    protected void openDatagram()
    throws SocketException {
        this.fd = posix.newUnixDatagram();
    }
    
    protected void openStream()
    throws SocketException {
        this.fd = posix.newUnixStream();
    }
    
    
    //----------------------------------------------------------------------
    public void bind(final UnixSocketAddress address)
    throws SocketException {
        closing.checkClosed(false);
        posix.bind(fd, address.getPath());
        this.localAddress = address;
    }
    
    public void connect(final UnixSocketAddress address)
    throws SocketException {
        closing.checkClosed(false);
        posix.connect(fd, address.getPath());
        this.remoteAddress = address;
    }
    
    public void disconnect() {
        //FIXME: What to do?
        this.remoteAddress = null;
    }
    
    public UnixSocket accept(final UnixServerSocket acceptor)
    throws SocketException {
        closing.checkClosed(false);
        final UnixSocketDescription childDesc = posix.acceptUnix(fd);
        if (childDesc == null) return null;
        closing.checkClosed(false);
        return new UnixSocket(
            acceptor,
            new UnixSocketImpl(
                childDesc.getFD(),
                localAddress,
                new UnixSocketAddress(childDesc.getClientAddress())));
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
    public void send(final GenericDatagramPacket p)
    throws IOException {
        closing.checkClosed(false);
        final SocketAddress address = p.getSocketAddress();
        if (address == null) {
            if (remoteAddress != null) {
                posix.send(fd, p.getData(), p.getOffset(), p.getLength());
            } else {
                throw new IllegalArgumentException
                    ("An unconnected Unix domain datagram socket"
                     + " cannot send packet " + p + " without explit address");
            }
        } else {
            if (address instanceof UnixSocketAddress) {
                posix.sendTo(fd, p.getData(), p.getOffset(), p.getLength(),
                             ((UnixSocketAddress)address).getPath());
            } else {
                throw new IllegalArgumentException
                    ("A Unix domain datagram socket can only send packages"
                     + " to a UnixSocketAddress, not to "
                     + address.getClass().getName());
            }
        }
    }
    
    public void receive(final GenericDatagramPacket p)
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
