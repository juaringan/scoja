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
import java.nio.channels.ServerSocketChannel;
import java.net.ServerSocket;

public class ConfigurableServerSocket extends TCPConf.Stacked {
    
    protected ServerSocketChannel socket;
    
    public ConfigurableServerSocket(final TCPConf conf) {
        super(conf);
        this.socket = null;
    }
    
    public ServerSocketChannel socket() { return socket; }
    public void socket(final ServerSocketChannel socket) {this.socket=socket;}
    
    public void configure(final ServerSocketChannel channel)
    throws IOException {    
        final ServerSocket s = channel.socket();
        if (timeout.has())
            s.setSoTimeout(timeout.get());
        if (receiveBufferSize.has())
            s.setReceiveBufferSize(receiveBufferSize.get());
        if (reuseAddress.has())
            s.setReuseAddress(reuseAddress.get());
    }
    
    public void setTimeout(final int time)
    throws IOException {
        super.setTimeout(time);
        if (socket != null) socket.socket().setSoTimeout(time);
    }
    
    public void setReceiveBufferSize(final int size)
    throws IOException {
        super.setReceiveBufferSize(size);
        if (socket != null) socket.socket().setReceiveBufferSize(size);
    }
    
    public void setReuseAddress(final boolean enabled)
    throws IOException {
        super.setReuseAddress(enabled);
        if (socket != null) socket.socket().setReuseAddress(enabled);
    }
}
