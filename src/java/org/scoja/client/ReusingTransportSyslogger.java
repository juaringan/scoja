/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2010  Mario Martínez
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
import java.util.Calendar;

import org.scoja.util.ByteFall;
import org.scoja.trans.Transport;
import org.scoja.trans.TransportLine;

public class ReusingTransportSyslogger
    extends StandardFormatter.SysloggerWith {
    
    protected final Transport trans;
    protected final ByteFall buffer;
    protected TransportLine line;

    public ReusingTransportSyslogger(final Transport trans) {
        this(trans, null);
    }
    
    public ReusingTransportSyslogger(final Transport trans,
            final ByteFall buffer) {
        this.trans = trans;
        this.buffer = buffer;
        this.line = null;
    }
    
    public synchronized void reset()
    throws LoggingException {
        if (line != null) {
            if (buffer != null) buffer.dropPartial();
            try {
                line.close();
            } catch (IOException e) {
                throw new LoggingException(e);
            } finally {
                line = null;
            }
        }
    }
    
    public synchronized void flush()
    throws LoggingException {
        if (line != null) {
            try {
                unloadBuffer();
                line.output().flush();
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
        formatter.format(when, priority, host, tag, message);
        formatter.addByAllMeans(terminator);
        final int len = formatter.toEnd() - formatter.fromPriority();
        int written = 0;
        Throwable error = null;
        try {
            ensureConnection();
            unloadBuffer();
            if (buffer == null || buffer.isEmpty())
                written = line.output().write(
                    formatter.getBytes(), formatter.fromPriority(), len);
        } catch (Throwable e) {
            error = e;
        }
        if (written < len && buffer != null) {
            buffer.add(formatter.getBytes(),
                    formatter.fromPriority()+written, len-written);
        }
        if (error != null) throw new LoggingException(error);
        else if (written < len && buffer == null) throw new LoggingException(
            "Message partially send ("
            + written + " bytes out of " + len + ")");
    }

    protected void ensureConnection()
    throws IOException {
        if (line == null) {
            line = trans.newClient();
            line.connect();
        }
    }
    
    protected void unloadBuffer()
    throws IOException {
        if (buffer != null && !buffer.isEmpty()) {
            while (buffer.unload(line.output()) > 0);
        }
    }
    
    //======================================================================
    public String toString() {
        return "[a reusing transport syslogger to " + trans
            + ", " + toStringDetails() + "]";
    }
}
