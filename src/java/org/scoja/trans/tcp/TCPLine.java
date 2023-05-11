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
import org.scoja.trans.*;
import org.scoja.trans.lc.*;

public abstract class TCPLine
    extends NoLCLine<TCPConf> {

    protected final ConfigurableSocket conf;

    public TCPLine(final TCPConf conf) {
        this.conf = new ConfigurableSocket(new TCPConf.Stacked(conf));
    }    
        
    public TCPLine(final TCPConf conf, final SocketChannel socket)
    throws IOException {
        this(conf);
        if (socket != null) {
            this.conf.configure(socket);
            this.conf.socket(socket);
        }
    }

    public String layers() { return "TCP"; }
        
    public TCPConf configuration() { return conf; }
    
    public boolean isBlocking() { return true; }

    public RemoteInfo remote() {
        return RemoteInfo.Inet.from(conf.socket());
    }
    
    public ConnectState connect()
    throws IOException {
        doConnect();
        return ConnectState.CONNECTED;
    }
    
    protected abstract void doConnect()
    throws IOException;
    
    public void close()
    throws IOException {
        final SocketChannel s = conf.socket();
        if (s != null) s.close();
    }
    
    public IStream input()
    throws IOException {
        return new IStream.FromInputStream(
            conf.socket().socket().getInputStream());
    }
    
    public OStream output()
    throws IOException {
        return new OStream.FromOutputStream(
            conf.socket().socket().getOutputStream());
    }
    
    public SelectionHandler register(final SelectionHandler handler) {
        //Nothing to do
        handler.addSelectable(this);
        return handler;
    }
    
    
    //======================================================================
    public static class Server extends TCPLine {
        
        protected final TCPService serv;
        
        public Server(final TCPService serv, final TCPConf.Stacked conf,
                final SocketChannel socket)
        throws IOException {
            super(conf, socket);
            this.serv = serv;
        }
        
        public void doConnect()
        throws IOException {
        }
    }
    
    //======================================================================
    public static class Client extends TCPLine {
        
        protected final TCPTransport trans;
        
        public Client(final TCPTransport trans, final TCPConf conf) {
            super(conf);
            this.trans = trans;
        }
        
        public void doConnect()
        throws IOException {
            SocketChannel s = null;
            try {
                s = SocketChannel.open();
                conf.configure(s);
                s.connect(trans.address);
                conf.socket(s); s = null;
            } finally {
                if (s != null) s.close();
            }
        }
    }
}
