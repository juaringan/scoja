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

package org.scoja.server.template;

import java.io.PrintWriter;
import java.util.Calendar;

import org.scoja.server.core.Event;
import org.scoja.server.core.EventContext;

public abstract class DateHoleSkeleton extends VariableHoleSkeleton {
    
    public static final int SEND_TIMESTAMP = 0;
    public static final int RECEPTION_TIMESTAMP = 1;
    public static final int PREFERRED_TIMESTAMP = 2;
    
    protected final int whichTimestamp;
    
    public DateHoleSkeleton(final String basicName,
                            final int whichTimestamp) {
        super(buildVarName(basicName, whichTimestamp));
        if (whichTimestamp < SEND_TIMESTAMP
            || whichTimestamp > PREFERRED_TIMESTAMP) {
            throw new IllegalArgumentException
                ("Value " + whichTimestamp
                 + " is not a valid id for an event timestamp");
        }
        this.whichTimestamp = whichTimestamp;
    }
    
    public Calendar getCalendar(final EventContext ectx) {
        final Event event = ectx.getEvent();
        switch (whichTimestamp) {
        case SEND_TIMESTAMP: return event.getSendCalendar();
        case RECEPTION_TIMESTAMP: return event.getReceptionCalendar();
        case PREFERRED_TIMESTAMP: return event.getPreferredCalendar();
        default: 
            //Cannot happen.
            return null;
        }
    }
    
    public int get(final EventContext ectx, final int key) {
        return getCalendar(ectx).get(key);
    }
    
    protected int getGMT(final EventContext ectx) {
        final Calendar date = getCalendar(ectx);
        return date.get(Calendar.ZONE_OFFSET) + date.get(Calendar.DST_OFFSET);
    }
    
    public static String buildVarName(final String basic,
                                      final int whichTimestamp) { 
        switch (whichTimestamp) {
        case SEND_TIMESTAMP: return "@" + basic;
        case RECEPTION_TIMESTAMP: return basic + "@";
        case PREFERRED_TIMESTAMP: return basic;
        default: 
            //Cannot happen.
            return basic;
        }
    }
}
