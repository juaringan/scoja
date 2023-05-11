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

import java.net.SocketAddress;
import java.net.InetSocketAddress;
import org.scoja.trans.*;
import org.scoja.trans.lc.*;
import org.scoja.trans.tcp.TCPConf;

/**
 * This is Non-Blocking TCP.
 */
public class NBTCPTransport 
    implements Transport<TCPConf> {

    protected final SocketAddress address;
    protected final TCPConf.Stacked conf;
    protected NBTCPService serv;
    
    public NBTCPTransport(final SocketAddress address) {
        this.address = address;
        this.conf = new TCPConf.Stacked();
        this.serv = null;
    }
    
    public NBTCPTransport(final String host, final int port) {
        this(new InetSocketAddress(host, port));
    }
    
    public boolean isBlocking() { return false; }
        
    public String layers() { return "NBTCP"; }
    
    public String endPointId() { return address.toString(); }
    
    public TCPConf configuration() { return conf; }
    
    public TransportLine<TCPConf> newClient() {
        return new LCLine<TCPConf>(new NBTCPLine.Client(this, conf));
    }
    
    public synchronized NBTCPService server() {
        if (serv == null) serv = new NBTCPService(this, conf);
        return serv;
    }
    
    public String toString() {
        return "NBTCPTransport[on: " + address
            + ", with: " + conf + "]";
    }
}
