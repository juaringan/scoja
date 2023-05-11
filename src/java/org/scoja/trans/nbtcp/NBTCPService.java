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
package org.scoja.trans.nbtcp;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;

import org.scoja.cc.lang.Exceptions;
import org.scoja.trans.InterestInterceptor;
import org.scoja.trans.TransportLine;
import org.scoja.trans.TransportService;
import org.scoja.trans.SelectionHandler;
import org.scoja.trans.tcp.TCPConf;
import org.scoja.trans.tcp.ConfigurableServerSocket;
import org.scoja.trans.lc.LCLine;

public class NBTCPService
    implements TransportService<TCPConf>, InterestInterceptor {

    protected final NBTCPTransport trans;
    protected final ConfigurableServerSocket conf;
    protected SelectionHandler sh;

    public NBTCPService(final NBTCPTransport trans, final TCPConf conf) {
        this.trans = trans;
        this.conf = new ConfigurableServerSocket(trans.conf);
        this.sh = null;
    }
    
    public TCPConf configuration() { return conf; }
    
    public boolean isBound()
    throws IOException {
        return conf.socket() != null;
    }
    
    public void bind()
    throws IOException {
        if (conf.socket() == null) {
            final ServerSocketChannel tmp = ServerSocketChannel.open();
            try {
                tmp.configureBlocking(false);
                conf.configure(tmp);
                tmp.socket().bind(trans.address);
                if (sh != null) sh.register(tmp);
            } catch (Throwable e) {
                try {tmp.close();} catch (Throwable ignored) {}
                throw Exceptions.uncheckedOr(IOException.class, e);
            }
            conf.socket(tmp);
        }
    }
    
    public void close()
    throws IOException {
        conf.socket().close();
    }
    
    public TransportLine<TCPConf> accept()
    throws IOException {
        bind();
        final SocketChannel s = conf.socket().accept();
        return new LCLine<TCPConf>(new NBTCPLine.Server(this, conf, s));
    }
    
    public SelectionHandler register(final SelectionHandler handler) {
        handler.addSelectable(this);
        handler.addInterestInterceptor(this);
        final ServerSocketChannel s = conf.socket();
        if (s != null) handler.register(s);
        return handler;
    }
    
    public int interestOps(final int current) {
        return SelectionKey.OP_ACCEPT;
    }
    
    public String toString() {
        return "NBTCPService[from: " + trans
            + ", with: " + conf
            + "]";
    }
}
