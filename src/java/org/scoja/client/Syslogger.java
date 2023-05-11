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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import org.scoja.common.PriorityUtils;
import org.scoja.cc.text.escaping.CLike;

/**
 * <p><b>Usage conventions</b>
 * All implementations of this abstract class, but {@link RetryingSyslogger},
 * should NOT recover automatically from errors.
 * They should raise an {@link LoggingException} and the using class will
 * do whatever it is appropriate.
 * Nevertheless, all subclasses must implement {@link #reset()} to help
 * user code to recover from an error.
 * Subclasses should be as lazy as possible; for instance, if a network
 * connection is needed, it should be opened when the first message is send,
 * not when the object is constructed.
 * With this policy, {@link #reset()} only has to close the socket and
 * reset everything to the state previous to the first send.
 * But subclasses could fail at construction time if a basic error is detected;
 * for instance, if host name is unresolvable or if the port number is
 * illegal.
 */
public abstract class Syslogger {
        
    public static final int NO_PACKET_LIMIT = -1;
    
    protected static int packetMin(final int s1, final int s2) {
        return (s1 == NO_PACKET_LIMIT) ? s2
            : (s2 == NO_PACKET_LIMIT) ? s1
            : Math.min(s1, s2);
    }
        
    public static final String ZERO = "\0";
    public static final String CR = "\r";
    public static final String LF = "\n";
    public static final String CRLF = CR + LF;
        
    protected int priority;
    protected String host;
    protected String tag;
    protected String terminatorStr;
    protected byte[] terminator;
    
    public Syslogger() {
        this.priority = PriorityUtils.DEFAULT_PRIORITY;
        try {
            this.host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            this.host = "unknown";
        }
        this.tag = "xcoja";
        this.terminatorStr = ZERO;
        this.terminator = ZERO.getBytes();
    }
    
    public Syslogger setPacketLimit(final int limit) {
        return this;
    }
    
    public int getPacketLimit() {
        return NO_PACKET_LIMIT;
    }
    
    public int getMessageBound() {
        return getMessageBound(NO_PACKET_LIMIT);
    }
    
    public int getMessageBound(final int n) {
        return n;
    }
    
    public Syslogger enableSendTimestamp(final boolean shouldSend) {
        return this;
    }
    
    public boolean isSendingTimestamp() {
        return true;
    }
    
    public Syslogger enableSendHost(final boolean shouldSend) {
        return this;
    }
    
    public boolean isSendingHost() {
        return true;
    }
    
    public Syslogger setFacility(final int facility) {
        return setPriority(PriorityUtils.setFacility(this.priority, facility));
    }
    
    public Syslogger setPriority(final int priority) {
        this.priority = priority;
        return this;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public int getFacility() {
        return PriorityUtils.getFacility(priority);
    }
    
    public int getLevel() {
        return PriorityUtils.getLevel(priority);
    }
    
    public Syslogger setHost(final String host) {
        this.host = host;
        return this;
    }
    
    public Syslogger setLocalHost()
    throws UnknownHostException {
        return setHost(InetAddress.getLocalHost().getHostName());
    }
    
    public Syslogger setCanonicalLocalHost()
    throws UnknownHostException {
        return setHost(InetAddress.getLocalHost().getCanonicalHostName());
    }
    
    public String getHost() {
        return host;
    }
    
    public Syslogger setTag(final String tag) {
        this.tag = tag;
        return this;
    }
    
    public String getTag() {
        return tag;
    }
    
    public Syslogger setTerminator(final String terminator) {
        this.terminatorStr = terminator;
        this.terminator = terminator.getBytes();
        return this;
    }
    
    public String getTerminator() {
        return terminatorStr;
    }
    
    public void reset()
    throws LoggingException {
    }
    
    public void flush()
    throws LoggingException {
    }
    
    public void close()
    throws LoggingException {
        flush();
        reset();
    }
    
    public java.io.OutputStream newOutputStream() {
        return new OutputStream(this);
    }
    
    public void log(final String message)
    throws LoggingException {
        log(PriorityUtils.UNKNOWN_PRIORITY, message);
    }
    
    public void log(final int priority, final String message)
    throws LoggingException {
        log(priority, null, null, message);
    }
    
    public void log(final int priority, final String host,
                    final String tag, final String message)
    throws LoggingException {
        log((Calendar)null, priority, host, tag, message);
    }
    
    public void log(final Date when, final int priority, final String host,
                    final String tag, final String message)
    throws LoggingException {
        final Calendar cal = Calendar.getInstance();
        if (when != null) cal.setTime(when);
        log(cal, getPriority(priority), getHost(host), getTag(tag), message);
    }
    
    public void log(final Calendar when, final int priority, final String host,
                    final String tag, final String message)
    throws LoggingException {
        ilog((when != null) ? when : Calendar.getInstance(),
             getPriority(priority), getHost(host), getTag(tag), message);
    }
    
    protected int getPriority(final int priority) {
        if (priority == PriorityUtils.UNKNOWN_PRIORITY) {
            return this.priority;
        } else if (PriorityUtils.getFacility(priority)
                   == PriorityUtils.NO_FACILITY) {
            return PriorityUtils.buildPriority(
                PriorityUtils.getFacility(this.priority),
                PriorityUtils.getLevel(priority));
        } else {
            return priority;
        }
    }
    
    protected String getHost(final String host) {
        return (host != null) ? host : this.host;
    }

    protected String getTag(final String tag) {
        return (tag != null) ? tag : this.tag;
    }

    /**
     * This method has the same signature and semantics than
     * {@link log(Calendar,int,String,String,String)},
     * but requires that all parameters are defined.
     * It substitutes no default value for the undefined parameters.
     */
    protected abstract void ilog(Calendar when, int priority, String host,
                                 String tag, String message)
    throws LoggingException;
    
    public String toStringDetails() {
        final int limit = getPacketLimit();
        return ( (limit == NO_PACKET_LIMIT) 
                 ? "with no packet limit"
                 : ("with packet limited to " + limit + " bytes") )
            + ", with default priority to "
            + PriorityUtils.getPriorityName(getPriority())
            + ( isSendingTimestamp()
                ? ", sending timestamp"
                : ", not sending timestamp" )
            + ( isSendingHost()
                ? ", sending host"
                : ", not sending host" )
            + ", with default host "
            + CLike.noControlSequence().escaped(getHost())
            + ", with default tag "
            + CLike.noControlSequence().escaped(getTag())
            + ", with default terminator "
            + CLike.noControlSequence().escaped(getTerminator());
    }
    
    
    //======================================================================
    /**
     * This is an adaptor from {@link Syslogger} to {@link OutputStream}.
     * It splits received data into lines and logs them to the given
     * {@link Syslogger}.
     * This class sends only data to the underlying syslogger; 
     * so, syslog messages have the facility, level, tag name, ...
     * of the syslogger configuration.
     */
    public class OutputStream extends java.io.OutputStream {
        
        protected final Syslogger logger;
        protected byte[] buffer;
        protected int used;
        
        public OutputStream(final Syslogger logger) {
            this.logger = logger;
            this.buffer = new byte[logger.getMessageBound(1024)];
            this.used = 0;
        }
        
        public void write(final int b) 
        throws IOException {
            if (used == 0 && b <= ' ') return;
            if (b == '\n' || b == '\r' || b == '\0') log();
            ensureCapacity(1);
            if (used < buffer.length) buffer[used++] = (byte)b;
        }
        
        public void write(final byte[] b, final int off, final int len)
        throws IOException {
            int i = off, limit = off+len, init = off;
            if (used > 0) {
                while (i < limit) {
                    if (b[i] == '\n' || b[i] == '\r' || b[i] == '\0') break;
                    i++;
                }
                addBuffer(b, init, i-init);
                if (i < limit) { 
                    log(); i++; 
                }
            }
            for (;;) {
                while (i < limit && b[i] <= ' ') i++;
                init = i;
                while (i < limit) {
                    if (b[i] == '\n' || b[i] == '\r' || b[i] == '\0') break;
                    i++;
                }
                if (i == limit) break;
                log(b, init, i-init);
                i++;
            }
            addBuffer(b, init, limit-init);
        }
        
        public void flush()
        throws IOException {
            if (used > 0) log();
            try {
                logger.flush();
            } catch (LoggingException e) {
                throw (IOException)new IOException().initCause(e);
            }
        }
        
        public void close()
        throws IOException {
            flush();
            try {
                logger.close();
            } catch (LoggingException e) {
                throw (IOException)new IOException().initCause(e);
            }
        }
        
        protected void addBuffer(final byte[] b, 
                                 final int off, final int len) {
            ensureCapacity(len);
            final int toCopy = Math.min(len, buffer.length-used);
            System.arraycopy(b,off, buffer,used,toCopy);
            used += toCopy;
        }
        
        protected void ensureCapacity(final int n) {
            if (used + n <= buffer.length) return;
            final int newlen = logger.getMessageBound(
                Math.max(2*buffer.length, used+n));
            if (newlen == buffer.length) return;
            final byte[] newbuffer = new byte[newlen];
            System.arraycopy(buffer,0, newbuffer,0,used);
            buffer = newbuffer;
        }
        
        protected void log()
        throws IOException {
            log(buffer, 0, used);
            used = 0;
        }
        
        protected void log(final byte[] b, final int off, final int len)
            throws IOException {
            try {
                final String message = new String(b, off, len);
                logger.log(message);
            } catch (LoggingException e) {
                throw (IOException)new IOException().initCause(e);
            }
        }
    }
}
