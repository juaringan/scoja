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

public interface LoggingErrorHandler {

    public void log(Syslogger logger, LoggingException error,
                    Calendar when, int priority, String host,
                    String tag, String message)
    throws LoggingException;
    
    public void ilog(Syslogger logger, LoggingException error,
                     Calendar when, int priority, String host,
                     String tag, String message)
    throws LoggingException;
    
    //======================================================================
    public class Ignorer
        implements LoggingErrorHandler {
        
        public void log(final Syslogger logger, final LoggingException error,
                        final Calendar when, 
                        final int priority, final String host,
                        final String tag, final String message)
        throws LoggingException {
        }
    
        public void ilog(final Syslogger logger, final LoggingException error,
                         final Calendar when, 
                         final int priority, final String host,
                         final String tag, final String message)
        throws LoggingException {
        }
    }
    
    //======================================================================
    public class Thrower
        implements LoggingErrorHandler {
        
        public void log(final Syslogger logger, final LoggingException error,
                        final Calendar when, 
                        final int priority, final String host,
                        final String tag, final String message)
        throws LoggingException {
            throw error;
        }
    
        public void ilog(final Syslogger logger, final LoggingException error,
                         final Calendar when, 
                         final int priority, final String host,
                         final String tag, final String message)
        throws LoggingException {
            throw error;
        }
    }
    
    //======================================================================
    public class ByPasser
        implements LoggingErrorHandler {
        
        protected final Syslogger base;
        
        public ByPasser(final Syslogger base) {
            this.base = base;
        }
        
        public void log(final Syslogger logger, final LoggingException error,
                        final Calendar when, 
                        final int priority, final String host,
                        final String tag, final String message)
        throws LoggingException {
            base.log(when, priority, host, tag, message);
        }
    
        public void ilog(final Syslogger logger, final LoggingException error,
                         final Calendar when, 
                         final int priority, final String host,
                         final String tag, final String message)
        throws LoggingException {
            base.ilog(when, priority, host, tag, message);
        }
    }
}
