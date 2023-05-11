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

package org.scoja.popu.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.scoja.common.DateLayout;

public class Format {

    protected final Locator eventEnd;
    protected final Locator dateStart;
    protected final Locator dateEnd;
    protected final DateLayout dateFormat;
    
    public Format(final Locator eventEnd,
                  final Locator dateStart,
                  final Locator dateEnd,
                  final String format) {
        this.eventEnd = eventEnd;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.dateFormat = DateLayout.Syslog.PATTERN.equals(format)
            ? (DateLayout)new DateLayout.Syslog()
            : (DateLayout)new DateLayout.JDK(new SimpleDateFormat(format));
    }
    
    public Locator getEventEnd() {
        return (Locator)eventEnd.clone();
    }
    
    public Locator getDateStart() {
        return (Locator)dateStart.clone();
    }
    
    public Locator getDateEnd() {
        return (Locator)dateEnd.clone();
    }
    
    public DateLayout getDateFormat() {
        return (DateLayout)dateFormat.clone();
    }
    
    //======================================================================
    public String toString() {
        return "Format["
            + dateStart.toPattern()
            + dateFormat.toPattern()
            + dateEnd.toPattern()
            + eventEnd.toPattern()
            + "]";
    }
}
