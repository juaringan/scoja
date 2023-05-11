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

package org.scoja.server.parser;

import java.io.PrintWriter;
import java.util.Calendar;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;

import org.scoja.common.PriorityUtils;
import org.scoja.common.DateUtils;
import org.scoja.trans.RemoteInfo;
import org.scoja.server.core.QStr;
import org.scoja.server.core.EventSkeleton;

public class ParsedEvent extends EventSkeleton {

    protected boolean parsed;
    protected final EventParser parser;
    protected final RemoteInfo peer;
    protected final String pack;
    protected boolean receptionChosen;
    protected Calendar sendCalendar;
    protected int priority;
    protected long sendTimestamp;
    protected String date;
    protected String host;
    protected String data;
    protected String program;
    protected String message;

    protected boolean hasExplicitHost;
    protected QStr qCanonicalHost;    

    public ParsedEvent(final RemoteInfo peer,
                       final byte[] pack) {
        this(peer, pack, 0, pack.length);
    }
    
    public ParsedEvent(final RemoteInfo peer,
                       final byte[] data, final int offset, final int length) {
        this(StdSyslogEventParser.getInstance(),
             peer, data, offset, length);
    }
    
    public ParsedEvent(final EventParser parser,
                       final RemoteInfo peer,
                       final byte[] data, final int offset, final int length) {
        super();
        this.parsed = false;
        this.parser = parser;
        this.peer = peer;
        this.pack = parser.trim(data, offset, length);
        
        this.receptionChosen = true;
        this.sendCalendar = null;
        this.priority = PriorityUtils.DEFAULT_PRIORITY;
        this.sendTimestamp = 0;
        this.date = null;
        this.host = null;
        this.data = "";
        this.program = "";
        this.message = "";
        
        this.hasExplicitHost = false;
        this.qCanonicalHost = null;
    }

    //======================================================================
    protected void ensureParsed() {
        if (!parsed) {
            try {
                parser.parseOn(this, pack);
            } finally {
                parsed = true;
            }
        }
    }

    protected void ensureSendDateParsed() {
        if (sendCalendar == null) {
            if (date != null) {
                sendCalendar = parser.parseDate(date);
            }
            if (sendCalendar == null) {
                sendCalendar = getReceptionCalendar();
                sendTimestamp = getReceptionTimestamp();
            } else {
                sendTimestamp = sendCalendar.getTimeInMillis();
            }
        }
    }
        
    public void setPriority(final int priority) {
        this.priority = priority;
    }
    
    public void setDate(final String date) {
        this.date = date;
    }
    
    public void setHost(final String host) {
        this.host = host;
        this.hasExplicitHost = true;
    }
            
    public void setData(final String data) {
        this.data = data;
    }
    
    public void setProgram(final String program) {
        this.program = program;
    }
    
    public void setMessage(final String message) {
        this.message = message;
    }


    //======================================================================
    public int getByteSize() {
        return pack.length();
    }
    
    public int getPriority() {
        ensureParsed();
        return priority;
    }
    
    public int getFacility() {
        return PriorityUtils.getFacility(getPriority());
    }
    
    public int getLevel() {
        return PriorityUtils.getLevel(getPriority());
    }
    
    public void chooseReceptionAsPreferredTimestamp(boolean choose) {
        this.receptionChosen = choose;
    }
    
    public long getPreferredTimestamp() {
        return receptionChosen ? getReceptionTimestamp() : getSendTimestamp();
    }
    
    public Calendar getPreferredCalendar() {
        return receptionChosen ? getReceptionCalendar() : getSendCalendar();
    }
    
    public long getSendTimestamp() {
        ensureParsed();
        ensureSendDateParsed();
        return sendTimestamp;
    }
    
    public Calendar getSendCalendar() {
        ensureParsed();
        ensureSendDateParsed();
        return sendCalendar;
    }
        
    public String getHost() {
        ensureParsed();
        if (host == null) {
            host = getAddress().getHostAddress();
        }
        return host;
    }
    
    public String getCanonicalHost() {
        return getQCanonicalHost().unqualified();
    }
    
    public QStr getQCanonicalHost() {
        if (qCanonicalHost == null) {
            if (hasExplicitHost) {
                try {
                    final String cname = InetAddress.getByName(getHost())
                        .getCanonicalHostName();
                    qCanonicalHost = new QStr(cname, QStr.HASNT_EOLN);
                } catch (UnknownHostException e) {}
            }
            if (qCanonicalHost == null) {
                qCanonicalHost = getQHost();
            }
        }
        return qCanonicalHost;
    }
    
    public InetAddress getAddress() {
        return peer.inetAddress();
    }
    
    public String getData() {
        ensureParsed();
        return data;
    }
    
    public String getProgram() {
        ensureParsed();
        return program;
    }
    
    public String getMessage() {
        ensureParsed();
        return message;
    }

    public Principal getPeerPrincipal() {
        return peer.principal();
    }
    
    public void writeTo(final PrintWriter out) {
        synchronized (out) {
            if (date != null && !receptionChosen) out.print(date);
            else try {
                    DateUtils.formatStd(out, getReceptionCalendar());
                } catch (java.io.IOException cannotHappen) {}
            out.print(' ');
            out.print(getHost());
            out.print(' ');
            out.print(data);
            out.write('\n');
        }
    }
}
