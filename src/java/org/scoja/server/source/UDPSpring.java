/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
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
package org.scoja.server.source;

import java.io.IOException;
import java.net.DatagramSocket;

public class UDPSpring implements Spring {

    public static final int DEFAULT_MAX_PACKET_SIZE = 2000;

    protected final String ip;
    protected final int port;
    protected int maxPacketSize;
    protected boolean reuseAddress;
    
    protected DatagramSocket socket;

    public UDPSpring(final String ip, final int port) {
        this.ip = ip;
        this.port = port;
        this.maxPacketSize = DEFAULT_MAX_PACKET_SIZE;
        this.reuseAddress = false;
        
        this.socket = null;
    }
    
    public void setMaxPacketSize(final int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }
    
    public void setReuseAddress(final boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public Spring.Id getId() {
        return null;
    } 
    
    public boolean isOpen() {
        return socket != null;
    }
    
    public void open() throws IOException {
        if (socket != null) return;
    }
    
    public void bind(final ContinuationProvider cont) {
        if (cont != null) ;
    }
    
    public void processEntry() {}
    
    public void unbind() {}
    
    public void close() throws IOException {}
}
