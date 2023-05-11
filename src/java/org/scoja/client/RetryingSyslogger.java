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

/**
 * Modifies a {@link Syslogger} to retry sending a message when an error
 * occurs.
 * If retries limit is reached, a notification is send to a
 * {@link LoggingErrorHandler}.
 */
public class RetryingSyslogger
    extends Syslogger {
    
    public static final int DEFAULT_RETRIES = 2;
    
    protected final Syslogger base;
    protected final int times;
    protected final LoggingErrorHandler handler;
    
    public static Syslogger with(final Syslogger base,
                                 final int times,
                                 final LoggingErrorHandler handler) {
        if (times <= 0 && handler == null) return base;
        else return new RetryingSyslogger(base, times, handler);
    }
    
    /**
     * Modifies <tt>base</tt> so that, when an error occurs,
     * it tries to send the message again {@link #DEFAULT_RETRIES} more times.
     * There is no {@link LoggingErrorHandler} to call if no try is successful.
     */
    public RetryingSyslogger(final Syslogger base) {
        this(base, DEFAULT_RETRIES, null);
    }
    
    /**
     * Modifies <tt>base</tt> so that, when an error occurs,
     * it tries to send the message again <tt>times</tt> more times.
     * If no try is successful and <tt>handler</tt> is not null,
     * then it is called with the last error and with the data to log.
     */
    public RetryingSyslogger(final Syslogger base,
                             final int times, 
                             final LoggingErrorHandler handler) {
        this.base = base;
        this.times = times;
        this.handler = handler;
    }
    
    public void reset()
    throws LoggingException {
        base.reset();
    }
    
    public void flush()
    throws LoggingException {
        base.flush();
    }
    
    public void log(final Calendar when, final int priority, final String host,
                    final String tag, final String message)
    throws LoggingException {
        int remain = times;
        LoggingException lastError = null;
        do {
            try {
                base.log(when, priority, host, tag, message);
                return;
            } catch (LoggingException e) {
                lastError = e;
                try { base.reset(); } catch (LoggingException ignored) {}
                remain--;
            }
        } while (remain >= 0);
        if (handler == null) {
            throw lastError;
        } else {
            handler.log(base, lastError, when, priority, host, tag, message);
        }
    }
    
    public void ilog(final Calendar when,
                     final int priority, final String host,
                     final String tag, final String message)
    throws LoggingException {
        int remain = times;
        LoggingException lastError = null;
        do {
            try {
                base.ilog(when, priority, host, tag, message);
                return;
            } catch (LoggingException e) {
                lastError = e;
                try { base.reset(); } catch (LoggingException ignored) {}
                remain--;
            }
        } while (remain > 0);
        if (handler == null) {
            throw lastError;
        } else {
            handler.ilog(base, lastError, when, priority, host, tag, message);
        }
    }
    
    
    //======================================================================
    public String toString() {
        return "[a retrying logger of " + base
            + ", trying " + times + " times"
            + ", handling errors with " + handler
            + ", " + toStringDetails();
    }
}
