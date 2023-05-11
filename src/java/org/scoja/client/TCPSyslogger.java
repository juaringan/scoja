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

import java.io.IOException;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;

public class TCPSyslogger
    extends StandardFormatter.SysloggerWith {
    
    protected final InetSocketAddress address;
    protected final TCPOptions options;
    
    public TCPSyslogger(final String host, final int port)
    throws UnknownHostException, SocketException {
        this(new InetSocketAddress(InetAddress.getByName(host), port));
    }
    
    public TCPSyslogger(final InetSocketAddress address)
    throws SocketException  {
        this.address = address;
        this.options = new TCPOptions();
    }
    
    public void setKeepAlive(final boolean keepAlive) {
        options.setKeepAlive(keepAlive, null);
    }
    public void setBufferSize(final int bufferSize) {
        options.setBufferSize(bufferSize, null);
    }
    public void setLinger(final int linger) {
        options.setLinger(linger, null);
    }
    public void setNoDelay(final boolean noDelay) {
        options.setNoDelay(noDelay, null);
    }
    
    protected void ilog(final Calendar when,
                        final int priority, final String host,
                        final String tag, final String message)
    throws LoggingException {
        try {
            Socket socket = null;
            try {
                socket = new Socket(address.getAddress(), address.getPort());
                options.configure(socket);
                synchronized (this) {
                    formatter.format(when, priority, host, tag, message);
                    formatter.addByAllMeans(terminator);
                    socket.getOutputStream().write(
                        formatter.getBytes(), 
                        formatter.fromPriority(),
                        formatter.toEnd() - formatter.fromPriority());
                }
            } finally {
                if (socket != null) socket.close();
            }
        } catch (IOException e) {
            throw new LoggingException(e);
        }
    }
    
    
    //======================================================================
    public String toString() {
        return "[a non-reusing TCP syslogger to " + address
            + ", " + toStringDetails() + "]";
    }
}
