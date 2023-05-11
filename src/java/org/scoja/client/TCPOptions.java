/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser/Library General Public License
 * as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
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

package org.scoja.client;

import java.net.Socket;
import java.net.SocketException;

public class TCPOptions {
    protected Boolean keepAlive;
    protected Integer bufferSize;
    protected Integer linger;
    protected Boolean noDelay;
    
    public TCPOptions() {
        this.keepAlive = null;
        this.bufferSize = null;
        this.linger = null;
        this.noDelay = null;
    }
    
    public void configure(final Socket socket) {
        if (keepAlive != null) confKeepAlive(socket, keepAlive.booleanValue());
        if (bufferSize != null) confBufferSize(socket, bufferSize.intValue());
        if (linger != null) confLinger(socket, linger.intValue());
        if (noDelay != null) confNoDelay(socket, noDelay.booleanValue());
    }
    
    public void setKeepAlive(final boolean keepAlive, final Socket socket) {
        if (this.keepAlive == null
            || this.keepAlive.booleanValue() != keepAlive) {
            if (socket != null) confKeepAlive(socket, keepAlive);
            this.keepAlive = Boolean.valueOf(keepAlive);
        }
    }
    
    public void setBufferSize(final int bufferSize, final Socket socket) {
        if (this.bufferSize == null
            || this.bufferSize.intValue() != bufferSize) {
            if (socket != null) confBufferSize(socket, bufferSize);
            this.bufferSize = new Integer(bufferSize);
        }
    }

    public void setLinger(final int linger, final Socket socket) {
        if (this.linger == null
            || this.linger.intValue() != linger) {
            if (socket != null) confLinger(socket, linger);
            this.linger = new Integer(linger);
        }
    }
    
    public void setNoDelay(final boolean noDelay, final Socket socket) {
        if (this.noDelay == null
            || this.noDelay.booleanValue() != noDelay) {
            if (socket != null) confNoDelay(socket, noDelay);
            this.noDelay = Boolean.valueOf(noDelay);
        }
    }
    
    protected void confKeepAlive(final Socket socket, final boolean val) {
        try { socket.setKeepAlive(val); } catch (SocketException e) {}
    }
    protected void confBufferSize(final Socket socket, final int val) {
        try { socket.setSendBufferSize(val); } catch (SocketException e) {}
    }
    protected void confLinger(final Socket socket, final int val) {
        try { socket.setSoLinger(val > 0, val); } catch (SocketException e) {}
    }
    protected void confNoDelay(final Socket socket, final boolean val) {
        try { socket.setTcpNoDelay(val); } catch (SocketException e) {}
    }
}
