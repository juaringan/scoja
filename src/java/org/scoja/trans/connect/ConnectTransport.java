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
package org.scoja.trans.connect;

import java.net.InetSocketAddress;
import org.scoja.cc.minihttp.HttpClientAuth;
import org.scoja.trans.*;
import org.scoja.trans.lc.LCLine;

public class ConnectTransport implements Transport<ConnectConf> {

    protected final Transport base;
    protected final InetSocketAddress targetAddress;
    protected final HttpClientAuth auth;
    protected final ConnectConf.Stacked conf;
    
    public ConnectTransport(final Transport base, 
            final InetSocketAddress targetAddress,
            final HttpClientAuth auth) {
        this.base = base;
        this.targetAddress = targetAddress;
        this.auth = auth;
        this.conf = new ConnectConf.Stacked();
    }
    
    public ConnectTransport(final Transport base,
            final String host, final int port,
            final HttpClientAuth auth) {
        this(base, new InetSocketAddress(host, port), auth);
    }
    
    public boolean isBlocking() { return base.isBlocking(); }
    
    public String layers() { return "connect-" + base.layers(); }
    
    public String endPointId() { 
        return targetAddress + "[" + base.endPointId() + "]";
    }

    public ConnectConf configuration() { return conf; }    

    public TransportLine<ConnectConf> newClient() {
        return new LCLine<ConnectConf>(new ConnectLine(this, conf));
    }
    
    public TransportService<ConnectConf> server() {
        //throw new UnsupportedOperationException();
        return new TransportService.TypeAdaptor<ConnectConf>(base.server());
    }
    
    public String toString() {
        return "ConnectTransport[on: " + base + ", with: " + conf + "]";
    }
}
