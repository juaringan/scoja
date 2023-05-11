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
import org.scoja.io.UnixSocket;
import org.scoja.io.UnixSocketAddress;

public class ReusingUnixStreamSyslogger
    extends StandardFormatter.SysloggerWith {
    
    protected final UnixSocketAddress address;
    protected UnixSocket socket;
    
    public ReusingUnixStreamSyslogger(final String filename)
    throws SocketException {
        this(new UnixSocketAddress(filename));
    }
    
    public ReusingUnixStreamSyslogger(final UnixSocketAddress address)
    throws SocketException  {
        this.address = address;
        this.socket = null;
    }
    
    public synchronized void reset()
    throws LoggingException {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new LoggingException(e);
            } finally {
                socket = null;
            }
        }
    }
    
    public synchronized void flush()
    throws LoggingException {
        if (socket != null) {
            try {
                socket.getOutputStream().flush();
            } catch (IOException e) {
                throw new LoggingException(e);
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
        try {
            ensureConnection();
            formatter.format(when, priority, host, tag, message);
            formatter.addByAllMeans(terminator);
            socket.getOutputStream().write(
                formatter.getBytes(), 
                formatter.fromPriority(),
                formatter.toEnd() - formatter.fromPriority());
        } catch (IOException e) {
            throw new LoggingException(e);
        }
    }

    protected void ensureConnection()
    throws IOException {
        if (socket == null) {
            socket = new UnixSocket(address);
        }
    }
    
    //======================================================================
    public String toString() {
        return "[a reusing Unix stream syslogger to " + address
            + ", " + toStringDetails() + "]";
    }
}
