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
import java.net.SocketException;
import java.util.Calendar;
import org.scoja.io.GenericDatagramPacket;
import org.scoja.io.UnixDatagramSocket;
import org.scoja.io.UnixSocketAddress;

public class UnixDatagramSyslogger
    extends StandardFormatter.SysloggerWith {
    
    protected final UnixSocketAddress address;
    protected final GenericDatagramPacket packet;
    protected UnixDatagramSocket socket;
    
    public UnixDatagramSyslogger(final String filename)
    throws SocketException {
        this(new UnixSocketAddress(filename));
    }
    
    public UnixDatagramSyslogger(final UnixSocketAddress address)
    throws SocketException  {
        this.address = address;
        this.packet = new GenericDatagramPacket(formatter.getBytes(), 0);
        this.socket = null;
    }
    
    public synchronized void reset()
    throws LoggingException {
        if (socket != null) {
            try { 
                socket.close(); 
            } catch (SocketException e) {
                throw new LoggingException(e);
            } finally {
                socket = null;
            }
        }
    }
    
    protected void finalize() {
        try { close(); } catch (LoggingException e) {}
    }
    
    protected synchronized void ilog(final Calendar when,
                                     final int priority, final String host,
                                     final String tag, final String message)
    throws LoggingException {
        formatter.format(when, priority, host, tag, message);
        formatter.addByAllMeans(terminator);
        try {
            ensureConnection();
            packet.setData(formatter.getBytes(), 
                           formatter.fromPriority(),
                           formatter.toEnd() - formatter.fromPriority());
            socket.send(packet);
        } catch (IOException e) {
            throw new LoggingException(e);
        }
    }
    
    protected void ensureConnection()
    throws SocketException {
        this.socket = new UnixDatagramSocket();
        this.socket.connect(address);
    }
    
    //======================================================================
    public String toString() {
        return "[a Unix Datagram syslogger to " + address
            + ", " + toStringDetails() + "]";
    }
}
