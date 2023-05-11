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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SpreadingSyslogger
    extends Syslogger {
    
    protected Syslogger[] nls;
    
    public SpreadingSyslogger(final Syslogger[] nls) {
        this.nls = nls;
    }
    
    public SpreadingSyslogger(final Syslogger sl1, final Syslogger sl2) {
        this.nls = new Syslogger[] {sl1, sl2};
    }
    
    public static SpreadingSyslogger spreadTo(final List/*<Syslogger>*/ sls) {
        return new SpreadingSyslogger(
            (Syslogger[])sls.toArray(new Syslogger[0]));
    }
    
    public void add(final Syslogger sl) {
        final Syslogger[] newnls = new Syslogger[nls.length+1];
        System.arraycopy(nls,0, newnls,0,nls.length);
        newnls[newnls.length-1] = sl;
        nls = newnls;
    }
    
    public Syslogger setPacketLimit(final int limit) {
        for (int i = 0; i < nls.length; i++) nls[i].setPacketLimit(limit);
        return this;
    }
    
    public int getPacketLimit() {
        int limit = NO_PACKET_LIMIT;
        for (int i = 0; i < nls.length; i++) {
            limit = packetMin(limit, nls[i].getPacketLimit());
        }
        return limit;
    }
    
    public int getMessageBound(final int n) {
        int limit = NO_PACKET_LIMIT;
        for (int i = 0; i < nls.length; i++) {
            limit = packetMin(limit, nls[i].getMessageBound(n));
        }
        return limit;
    }
    
    public Syslogger enableSendTimestamp(final boolean shouldSend) {
        for (int i = 0; i < nls.length; i++) {
            nls[i].enableSendTimestamp(shouldSend);
        }
        return this;
    }
    
    public boolean isSendingTimestamp() {
        for (int i = 0; i < nls.length; i++) {
            if (!nls[i].isSendingTimestamp()) return false;
        }
        return true;
    }    
    
    public Syslogger enableSendHost(final boolean shouldSend) {
        for (int i = 0; i < nls.length; i++) {
            nls[i].enableSendHost(shouldSend);
        }
        return this;
    }
    
    public boolean isSendingHost() {
        for (int i = 0; i < nls.length; i++) {
            if (!nls[i].isSendingHost()) return false;
        }
        return true;
    }
    
    public Syslogger setPriority(final int priority) {
        super.setPriority(priority);
        for (int i = 0; i < nls.length; i++) nls[i].setPriority(priority);
        return this;
    }
    
    public Syslogger setHost(final String host) {
        super.setHost(host);
        for (int i = 0; i < nls.length; i++) nls[i].setHost(host);
        return this;
    }
    
    public Syslogger setTag(final String tag) {
        super.setTag(tag);
        for (int i = 0; i < nls.length; i++) nls[i].setTag(tag);
        return this;
    }
    
    public Syslogger setTerminator(final String terminator) {
        super.setTerminator(terminator);
        for (int i = 0; i < nls.length; i++) nls[i].setTerminator(terminator);
        return this;
    }
    
    public void reset()
    throws LoggingException {
        for (int i = 0; i < nls.length; i++) nls[i].reset();
    }
    
    public void flush()
    throws LoggingException {
        for (int i = 0; i < nls.length; i++) nls[i].flush();
    }
    
    public void log(final Date when, final int priority, final String host,
                    final String tag, final String message)
    throws LoggingException {
        final Date rwhen = (when != null) ? when : new Date();
        for (int i = 0; i < nls.length; i++) {
            nls[i].log(rwhen, priority, host, tag, message);
        }
    }
    
    public void log(final Calendar when, final int priority, final String host,
                    final String tag, final String message)
    throws LoggingException {
        final Calendar rwhen = (when != null) ? when : Calendar.getInstance();
        for (int i = 0; i < nls.length; i++) {
            nls[i].log(rwhen, priority, host, tag, message);
        }
    }
    
    protected void ilog(final Calendar when,
                        final int priority, final String host,
                        final String tag, final String message)
    throws LoggingException {
        for (int i = 0; i < nls.length; i++) {
            nls[i].ilog(when, priority, host, tag, message);
        }
    }
    
    //======================================================================
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("[a spreding syslogger composed of ");
        for (int i = 0; i < nls.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(nls[i]);
        }
        sb.append(", ").append(toStringDetails());
        return sb.toString();
    }
}
