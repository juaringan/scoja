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

/**
 * This is the basic implementation for all sockets, either Unix domain
 * or Internet domain.
 */
abstract class SocketImpl {

    protected final PosixLike posix;

    protected int fd;
    protected final Closing closing;
    
    protected SocketImpl(final int fd) {
        this.posix = Posix.getPosix();
        this.fd = fd;
        this.closing = new Closing(this);
    }

    protected int getFD() {
        return fd;
    }
    
    
    //----------------------------------------------------------------------
    public void listen(final int incomingQueueLimit)
    throws SocketException {
        closing.checkClosed(false);
        posix.listen(fd, incomingQueueLimit);
    }
    
    
    //----------------------------------------------------------------------
    public boolean isClosed() {
        return closing.isClosed();
    }
    
    public abstract SocketAddress getRemoteSocketAddress();
        
    public abstract SocketAddress getLocalSocketAddress();
    
    
    //----------------------------------------------------------------------
    public void send(final int b) throws IOException {
        closing.checkClosed(false);
        posix.send(fd, b);
    }
    
    public int send(final byte[] buf, final int off, final int len)
    throws IOException {
        closing.checkClosed(false);
        return posix.send(fd, buf, off, len);
    }
    
    public int receive()
    throws IOException {
        closing.checkClosed(false);
        return posix.receive(fd);
    }
    
    public int receive(final byte[] b, final int off, final int len)
    throws IOException {
        closing.checkClosed(false);
        return posix.receive(fd, b, off, len);
    }
    
    public void flush()
    throws IOException {
        closing.checkClosed(false);
        //posix.flush(fd);
    }
    
    public void shutdown(final boolean input)
    throws IOException {
        closing.checkClosed(false);
        posix.shutdown(fd, input ? PosixLike.READ_HALF : PosixLike.WRITE_HALF);
    }
    
    public void close()
    throws SocketException {
        if (!closing.close()) return;
        close0();
    }
    
    protected void close0()
    throws SocketException {
        try {
            posix.close(fd);
        } catch (SocketException e) {
            throw e;
        } catch (IOException e) {
            throw (SocketException)
                new SocketException(e.getMessage()).initCause(e);
        }
        fd = -1;
    }

    
    //----------------------------------------------------------------------
    public void setDebug(final boolean on)
    throws SocketException {
        closing.checkClosed(false);
        posix.setDebug(fd, on);
    }
    
    public boolean getDebug()
    throws SocketException {
        closing.checkClosed(false);
        return posix.getDebug(fd);
    }
    
    public void setBroadcast(final boolean on)
    throws SocketException {
        closing.checkClosed(false);
        posix.setBroadcast(fd, on);
    }

    public boolean getBroadcast()
    throws SocketException {
        closing.checkClosed(false);
        return posix.getBroadcast(fd);
    }
        
    public void setKeepAlive(final boolean on)
    throws SocketException {
        closing.checkClosed(false);
        posix.setKeepAlive(fd, on);
    }
        
    public boolean getKeepAlive()
    throws SocketException {
        closing.checkClosed(false);
        return posix.getKeepAlive(fd);
    }
    
    public void setOOBInline(final boolean on)
    throws SocketException {
        closing.checkClosed(false);
        posix.setOOBInline(fd, on);
    }

    public boolean getOOBInline()
    throws SocketException {
        closing.checkClosed(false);
        return posix.getOOBInline(fd);
    }
    
    public void setSoTimeout(final long timeout)
    throws SocketException {
        closing.checkClosed(false);
        posix.setReadTimeout(fd, timeout);
    }

    public long getSoTimeout()
    throws SocketException {
        closing.checkClosed(false);
        return posix.getReadTimeout(fd);
    }
    
    public void setReceiveBufferSize(final int size)
    throws SocketException {
        closing.checkClosed(false);
        posix.setReceiveBufferSize(fd, size);
    }

    public int getReceiveBufferSize()
    throws SocketException {
        closing.checkClosed(false);
        return posix.getReceiveBufferSize(fd);
    }

    public void setReuseAddress(final boolean reuse)
    throws SocketException {
        closing.checkClosed(false);
        posix.setReuseAddress(fd, reuse);
    }

    public boolean getReuseAddress()
    throws SocketException {
        closing.checkClosed(false);
        return posix.getReuseAddress(fd);
    }
        
    public void setSendBufferSize(final int size)
    throws SocketException {
        closing.checkClosed(false);
        posix.setSendBufferSize(fd, size);
    }
    
    public int getSendBufferSize()
    throws SocketException {
        closing.checkClosed(false);
        return posix.getSendBufferSize(fd);
    }
    
    public void setSoLinger(final boolean on, final int linger)
    throws SocketException {
        closing.checkClosed(false);
        posix.setSoLinger(fd, on, linger);
    }
    
    public int getSoLinger()
    throws SocketException {
        closing.checkClosed(false);
        return posix.getSoLinger(fd);
    }
    
    public void setTcpNoDelay(final boolean on)
    throws SocketException {
        closing.checkClosed(false);
        posix.setTcpNoDelay(fd, on);
    }
    
    public boolean getTcpNoDelay()
    throws SocketException {
        closing.checkClosed(false);
        return posix.getTcpNoDelay(fd);
    }
    
        
    //======================================================================
    protected void finalize() {
        try { close(); } catch (SocketException e) {}
    }    
    
    public void getAttributes(final StringBuffer sb) {
        if (isClosed()) {
            sb.append("closed");
        } else {
            boolean first = true;
            sb.append("fd: ").append(fd);
            first = false;
            final SocketAddress local = getLocalSocketAddress();
            if (local != null) {
                if (!first) sb.append(", ");
                sb.append("bound to: ").append(local);
                first = false;
            }
            final SocketAddress remote = getRemoteSocketAddress();
            if (remote != null) {
                if (!first) sb.append(", ");
                sb.append("connected to: ").append(remote);
                first = false;
            }
        }
    }
}
