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
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.scoja.cc.lang.Exceptions;
import org.scoja.trans.*;
import org.scoja.trans.lc.*;

public class TCPService
    implements TransportService<TCPConf> {

    protected final TCPTransport trans;
    protected final ConfigurableServerSocket conf;

    public TCPService(final TCPTransport trans, final TCPConf conf) {
        this.trans = trans;
        this.conf = new ConfigurableServerSocket(conf);
    }
    
    public TCPConf configuration() { return conf; }

    public boolean isBound()
    throws IOException {
        final ServerSocketChannel ss = conf.socket();
        return ss != null && ss.socket().isBound();
    }
    
    public void bind()
    throws IOException {
        if (conf.socket() == null) {
            final ServerSocketChannel tmp = ServerSocketChannel.open();
            try {
                conf.configure(tmp);
                tmp.socket().bind(trans.address);
            } catch (Throwable e) {
                try {tmp.close();} catch (Throwable ignored) {}
                throw Exceptions.uncheckedOr(IOException.class, e);
            }
            conf.socket(tmp);
        }
    }
    
    public void close()
    throws IOException {
        final ServerSocketChannel ss = conf.socket();
        if (ss != null) {
            ss.close();
            conf.socket(null);
        }
    }
    
    public TransportLine<TCPConf> accept()
    throws IOException {
        bind();
        final SocketChannel s = conf.socket().accept();
        return new LCLine<TCPConf>(new TCPLine.Server(this, conf, s));
    }
    
    public SelectionHandler register(final SelectionHandler handler) {
        //Nothing to do
        handler.addSelectable(this);
        return handler;
    }
    
    public String toString() {
        return "TCPService[from: " + trans
            + ", with: " + conf
            + "]";
    }
}
