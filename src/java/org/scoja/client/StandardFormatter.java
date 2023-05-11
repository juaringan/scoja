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
import org.scoja.common.DateLayout;

public class StandardFormatter {

    private static final int MIN_PRIORITY_WIDTH = 1+1+1;
    private static final int MAX_PRIORITY_WIDTH = 1+10+1;
    public static final int NO_PACKET_LIMIT = Syslogger.NO_PACKET_LIMIT;
    public static final int MIN_PACKET_LIMIT = 256;

    protected byte[] buffer;
    protected int priorityInit;
    protected int lastPriority;
    protected int lastPriorityInit;
    protected int dateEnd;
    protected long lastDate;
    protected int next;
    
    protected boolean sendDate;
    protected boolean sendHost;
    protected int packetLimit;
    
    public StandardFormatter() {
        this.buffer = new byte[1024];
        this.lastPriority = -1;
        this.lastDate = Long.MIN_VALUE;
        this.lastPriorityInit = this.priorityInit
            = this.dateEnd = this.next = MAX_PRIORITY_WIDTH;
        this.sendDate = true;
        this.sendHost = true;
        this.packetLimit = NO_PACKET_LIMIT;
    }
    
    public void enableSendTimestamp(final boolean enable) {
        this.sendDate = enable;
    }
    
    public boolean isSendingTimestamp() {
        return sendDate;
    }
    
    public void enableSendHost(final boolean enable) {
        this.sendHost = enable;
    }
    
    public boolean isSendingHost() {
        return sendHost;
    }
    
    public void setPacketLimit(final int limit) {
        this.packetLimit
            = (limit == NO_PACKET_LIMIT || limit >= MIN_PACKET_LIMIT)
            ? limit : MIN_PACKET_LIMIT;
    }
    
    public int getPacketLimit() {
        return packetLimit;
    }
    
    public int getMessageBound(final int max) {
        return (packetLimit == NO_PACKET_LIMIT)
            ? max
            : (packetLimit - MIN_PRIORITY_WIDTH - DateLayout.Syslog.WIDTH - 3);
    }
    
    public int currentSize() {
        return toEnd() - fromPriority();
    }
    
    public void reset() {
        this.priorityInit = this.dateEnd = this.next = MAX_PRIORITY_WIDTH;
    }
    
    public boolean isUnlimited() {
        return packetLimit == NO_PACKET_LIMIT;
    }
    
    public boolean isLimited() {
        return packetLimit != NO_PACKET_LIMIT;
    }
    
    public byte[] getBytes() {
        return buffer;
    }
    
    public int fromPriority() {
        return priorityInit;
    }
    
    public int fromDate() {
        return MAX_PRIORITY_WIDTH;
    }
    
    public int toEnd() {
        return next;
    }
    
    public void format(final Calendar when,
                       final int priority, final String host,
                       final String tag, final String message) {
        format(when, priority, sendHost ? host.getBytes() : null,
               tag.getBytes(), message.getBytes());
    }
    
    public void format(final Calendar when,
                       final int priority, final byte[] host,
                       final byte[] tag, final byte[] message) {
        if (priority == lastPriority) {
            this.priorityInit = this.lastPriorityInit;
        } else {
            //System.err.println("Rebuilding priority");
            int i = MAX_PRIORITY_WIDTH;
            buffer[--i] = (byte)'>';
            if (priority <= 0) {
                buffer[--i] = (byte)'0';
            } else {
                int p = priority;
                while (p > 0) {
                    buffer[--i] = (byte)('0' + (p % 10));
                    p /= 10;
                }
            }
            buffer[--i] = (byte)'<';
            this.lastPriority = priority;
            this.lastPriorityInit = this.priorityInit = i;
        }
        
        if (sendDate) {
            //final long millis = when.getTimeInMillis();
            // Port to 1.3.
            final long millis = when.getTime().getTime();
            if (lastDate/1000 != millis/1000) {
                //System.err.println("Rebuilding time");
                DateLayout.Syslog.getInstance()
                    .formatTo(buffer, MAX_PRIORITY_WIDTH, when);
                lastDate = millis;
                buffer[MAX_PRIORITY_WIDTH+DateLayout.Syslog.WIDTH] = (byte)' ';
            }
            this.dateEnd = MAX_PRIORITY_WIDTH + DateLayout.Syslog.WIDTH + 1;
        }
        
        this.next = this.dateEnd;
        ensureCapacity((sendHost ? host.length : 0)
                       + 1 + tag.length + 2 + message.length);
        if (sendHost) { add0(host); add0(' '); }
        add0(tag); add0(':'); add0(' '); add0(message);
    }
    
    public void addByAllMeans(final String terminator) {
        addByAllMeans(terminator.getBytes());
    }
    
    public void addByAllMeans(final byte[] terminator) {
        if (isLimited()) {
            next = Math.min(
                next, priorityInit + packetLimit - terminator.length);
        }
        ensureCapacity(terminator.length);
        add0(terminator);
    }
    
    protected void add0(final int b) {
        if (next < buffer.length) buffer[next++] = (byte)b;
    }
    
    protected void add0(final byte[] bs) {
        final int n = Math.min(bs.length, buffer.length-next);
        System.arraycopy(bs,0, buffer,next,n);
        next += n;
    }
    
    protected void ensureCapacity(final int n) {
        final int ln = (packetLimit == NO_PACKET_LIMIT) ? n
            : Math.min(n, packetLimit - (next - priorityInit));
        if (ln <= buffer.length - next) return;
        final int newlen = (packetLimit == NO_PACKET_LIMIT)
            ? Math.max(2*buffer.length, next+ln)
            : (MAX_PRIORITY_WIDTH + packetLimit);
        //System.err.println("Resizing to " + newlen);
        final byte[] newBuffer = new byte[newlen];
        System.arraycopy(buffer,0, newBuffer,0,next);
        buffer = newBuffer;
    }
    
    
    //======================================================================
    public static abstract class SysloggerWith
        extends Syslogger {
        
        protected final StandardFormatter formatter;
        
        public SysloggerWith() {
            this.formatter = new StandardFormatter();
        }
        
        public Syslogger setPacketLimit(final int limit) {
            formatter.setPacketLimit(limit);
            return this;
        }
        
        public int getPacketLimit() {
            return formatter.getPacketLimit();
        }
        
        public int getMessageBound(final int n) {
            return formatter.getMessageBound(n);
        }
        
        public Syslogger enableSendTimestamp(final boolean shouldSend) {
            formatter.enableSendTimestamp(shouldSend);
            return this;
        }
        
        public boolean isSendingTimestamp() {
            return formatter.isSendingTimestamp();
        }
    
        public Syslogger enableSendHost(final boolean shouldSend) {
            formatter.enableSendHost(shouldSend);
            return this;
        }
        
        public boolean isSendingHost() {
            return formatter.isSendingHost();
        }
    }
}
