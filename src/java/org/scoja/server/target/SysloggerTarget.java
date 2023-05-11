/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
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

package org.scoja.server.target;

import java.util.Calendar;
import java.util.Date;

import org.scoja.client.LoggingException;
import org.scoja.client.Syslogger;
import org.scoja.common.PriorityUtils;
import org.scoja.server.core.EventContext;
import org.scoja.server.core.Link;
import org.scoja.server.expr.Host;
import org.scoja.server.expr.Message;
import org.scoja.server.expr.Program;
import org.scoja.server.template.EventWriter;
import org.scoja.server.template.StringHole;
import org.scoja.server.source.Internal;

public class SysloggerTarget
    extends Link {
    
    public static final EventWriter MESSAGE
        = new StringHole("MESSAGE", new Message());
    public static final EventWriter TAG
        = new StringHole("PROGRAM", new Program());
    public static final EventWriter HOST
        = new StringHole("HOST", new Host());
        
    protected final Syslogger logger;
    protected EventWriter message;
    protected EventWriter tag;
    protected EventWriter host;

    protected int retries;
    protected long inactivityOnError;    
    protected volatile boolean disabled;
    protected long disabledTo;
    
    public SysloggerTarget(final Syslogger logger) {
        this.logger = logger;
        this.message = MESSAGE;
        this.tag = TAG;
        this.host = HOST;
        
        this.retries = 3;
        this.inactivityOnError = 2*1000;
        this.disabled = false;
        this.disabledTo = Long.MIN_VALUE;
    }
    
    public void setPacketLimit(final int limit) {
        logger.setPacketLimit(limit);
    }
    
    public void setMessage(final EventWriter message) {
        this.message = message;
    }
    
    public void setTag(final EventWriter tag) {
        this.tag = tag;
    }
    
    public void setHost(final EventWriter host) {
        this.host = host;
        logger.enableSendHost(host != null);
    }
    
    public void setTerminator(final String eom) {
        logger.setTerminator(eom);
    }
    
    public void setRetries(final int retries) {
        this.retries = retries;
    }
    
    public void setInactivityOnError(final long inactivity) {
        this.inactivityOnError = inactivity;
    }
    
    public void process(final EventContext ectx) {
        if (disabled) {
            synchronized (this) {
                if (System.currentTimeMillis() < disabledTo) return;
                disabled = false;
                Internal.crit(ectx, Internal.TARGET_SYSLOG,
                              "Enabling " + this);
            }
        }
        final int effectivePriority = ectx.getEvent().getPriority();
        final String effectiveHost = getFor(host, ectx);
        final String effectiveTag = tag.textFor(ectx);
        final String effectiveMessage = message.textFor(ectx);
        int remain = retries;
        do {
            try {
                logger.log((Calendar)null, effectivePriority, 
                           effectiveHost, effectiveTag, effectiveMessage);
                logger.flush();
                return;
            } catch (Throwable e) {
                Internal.err(ectx, Internal.TARGET_SYSLOG,
                             "Error while sending with " + this, e);
                remain--;
                try {
                    logger.reset();
                } catch (Throwable e2) {
                    Internal.err(ectx, Internal.TARGET_SYSLOG,
                                 "Error while reseting " + this, e2);
                }
            }
        } while (remain > 0);
        if (remain == 0 && inactivityOnError > 0) {
            synchronized (this) {
                disabled = true;
                disabledTo = System.currentTimeMillis() + inactivityOnError;
                Internal.crit(ectx, Internal.TARGET_SYSLOG, "Disabling " + this
                              + " upto " + new Date(disabledTo));
            }
        }
    }
    
    protected String getFor(final EventWriter ew, final EventContext ectx) {
        return (ew == null) ? null : ew.textFor(ectx);
    }
    
    //======================================================================
    public String toString() {
        return "[a syslog sender through " + logger + "]";
    }
}
