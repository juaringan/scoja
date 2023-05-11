/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2012  LogTrust
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

package org.scoja.server.netflow;

import java.io.PrintWriter;
import java.util.Calendar;
import java.net.InetAddress;
import java.security.Principal;

import org.scoja.trans.RemoteInfo;
import org.scoja.common.DateUtils;
import org.scoja.common.PriorityUtils;
import org.scoja.server.core.EventSkeleton;
import org.scoja.server.core.QStr;

public class NetflowEvent extends EventSkeleton {

    public static final int PRIORITY = PriorityUtils.buildPriority(
        PriorityUtils.DAEMON, PriorityUtils.INFO);
    public static final QStr PROGRAM = QStr.supposePerfect("netflow");

    protected final RemoteInfo peer;
    protected final NetflowEnvironment env;
    protected boolean receptionChosen;
    protected Calendar sendCalendar;

    public NetflowEvent(final RemoteInfo peer, final NetflowEnvironment env) {
        this.peer = peer;
        this.env = env;
        this.qProgram = PROGRAM;
        this.receptionChosen = true;
        this.sendCalendar = null;
    }
    

    //======================================================================    
    public int getByteSize() { return env.byteSize(); }
    
    public int getPriority() { return PRIORITY; }
    
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
        return env.sendTimestamp();
    }
    
    public Calendar getSendCalendar() {
        if (sendCalendar == null) {
            sendCalendar = Calendar.getInstance();
            sendCalendar.setTimeInMillis(getSendTimestamp());
        }
        return sendCalendar;
    }    
    
    //======================================================================
    public InetAddress getAddress() { return peer.inetAddress(); }
    
    public String getHost() { return getAddress().getHostAddress(); }
    
    public String getCanonicalHost() {
        return getAddress().getCanonicalHostName();
    }
    
    public QStr getQCanonicalHost() {
        return QStr.unchecked(getCanonicalHost());
    }
    
    public String getProgram() {
        return PROGRAM.unqualified();
    }
    
    public String getMessage() {
        final StringBuilder sb = new StringBuilder();
        try {
            env.writeTo(sb);
        } catch (java.io.IOException cannotHappen) {}
        return sb.toString();
    }
    
    public String getData() {
        return getProgram() + ": " + getMessage();
    }    
    
    public Principal getPeerPrincipal() {
        return peer.principal();
    }
    
    public void writeTo(final PrintWriter out) {
        try {
            DateUtils.formatStd(out, getPreferredCalendar());
            out.print(' ');
            out.print(getHost());
            out.print(" netflow: ");
            env.writeTo(out);
            out.write('\n');
        } catch (java.io.IOException cannotHappen) {}
    }
}
