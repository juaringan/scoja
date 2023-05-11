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

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This is just the interface of java.net.InetSocketAddress, with the
 * same name, with the behaviour described at the javadoc,
 * but in another package and implemented from scratch
 * (it is almost trivial).
 * Just to port Scoja clients to j2se 1.3; see {@link SocketAddress}.
 */
public class InetSocketAddress extends SocketAddress {

    protected InetAddress addr;
    protected String hostname;
    protected final int port;
    
    public InetSocketAddress(final int port) {
        this("0.0.0.0", port);
    }
    
    public InetSocketAddress(final InetAddress addr, final int port) {
        this.addr = addr;
        this.hostname = addr.getHostName();
        this.port = port;
    }
    
    public InetSocketAddress(final String hostname, final int port) {
        if (port < 0 || 0xFFFF < port) {
            throw new IllegalArgumentException("Illegal ort number " + port);
        }
        try {
            this.addr = InetAddress.getByName(hostname);
            this.hostname = this.addr.getHostName();
        } catch (UnknownHostException e) {
            this.addr = null;
            this.hostname = hostname;
        }
        this.port = port;
    }
    
    public int getPort() { return port; }
    public InetAddress getAddress() { return addr; }
    public String getHostName() { return hostname; }
    public boolean isUnresolved() { return addr == null; }
    
    //======================================================================
    public String toString() { return hostname + ":" + port; }
    
    public boolean equals(final Object other) {
        return (other instanceof InetSocketAddress)
            && equals((InetSocketAddress)other);
    }
    
    public boolean equals(final InetSocketAddress other) {
        return other != null
            && this.getPort() == other.getPort()
            && ( (this.getAddress() == null) 
                 ? (other.getAddress() == null)
                 : this.getAddress().equals(other.getAddress())
                 );
    }
}
