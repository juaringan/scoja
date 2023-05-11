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
package org.scoja.trans.tcp;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.Socket;

public class ConfigurableSocket extends TCPConf.Proxy {
    
    protected SocketChannel socket;
    
    public ConfigurableSocket(final TCPConf base) {
        super(base);
        this.socket = null;
    }
    
    public SocketChannel socket() { return socket; }
    public void socket(final SocketChannel socket) { this.socket = socket; }

    public void configure(final SocketChannel channel)
    throws IOException {
        final Socket s = channel.socket();
        if (getTimeout().has())
            s.setSoTimeout(getTimeout().get());
        /* No use for client sockets
        if (getReuseAddress().has())
            s.setReuseAddress(getReuseAddress().get());
        */
        if (getSendBufferSize().has())
            s.setSendBufferSize(getSendBufferSize().get());
        if (getReceiveBufferSize().has())
            s.setReceiveBufferSize(getReceiveBufferSize().get());
        if (getKeepAlive().has()) 
            s.setKeepAlive(getKeepAlive().get());
        if (getNoDelay().has())
            s.setTcpNoDelay(getNoDelay().get());
        if (getLinger().has())
            setLinger(s, getLinger().get());
        if (getTrafficClass().has())
            s.setTrafficClass(getTrafficClass().get());
    }
    
    public void setTimeout(final int time)
    throws IOException {
        super.setTimeout(time);
        if (socket != null) socket.socket().setSoTimeout(time);
    }

    /* No use for client sockets.
    public void setReuseAddress(final boolean enabled)
    throws IOException {
        super.setReuseAddress(enabled);
        if (socket != null) socket.socket().setReuseAddress(enabled);
    }
    */
    
    public void setSendBufferSize(final int size)
    throws IOException {
        super.setSendBufferSize(size);
        if (socket != null) socket.socket().setSendBufferSize(size);
    }
    
    public void setReceiveBufferSize(final int size)
    throws IOException {
        super.setReceiveBufferSize(size);
        if (socket != null) socket.socket().setReceiveBufferSize(size);
    }
    
    public void setKeepAlive(final boolean enabled)
    throws IOException {
        super.setKeepAlive(enabled);
        if (socket != null) socket.socket().setKeepAlive(enabled);
    }
    
    public void setNoDelay(final boolean enabled)
    throws IOException {
        super.setNoDelay(enabled);
        if (socket != null) socket.socket().setTcpNoDelay(enabled);
    }
    
    public void setLinger(final int linger)
    throws IOException {
        super.setLinger(linger);
        if (socket != null) setLinger(socket.socket(), linger);
    }
    
    public static void setLinger(final Socket socket, final int linger)
    throws IOException {
        if (linger < 0) socket.setSoLinger(false, 0);
        else socket.setSoLinger(true, linger);
    }
    
    public void setTrafficClass(final int traffic)
    throws IOException {
        base.setTrafficClass(traffic);
        if (socket != null) socket.socket().setTrafficClass(traffic);
    }
}
