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
package org.scoja.client.jul;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.LogRecord;

public class Timestamp extends Hole {
    
    public static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    
    protected String format;
    
    public Timestamp() {
        super("date");
        this.format = DEFAULT_FORMAT;
    }
    
    public void with(final String args)
    throws IllegalArgumentException {
        final String cleaned = args.trim();
        if (cleaned.length() > 0) {
            new SimpleDateFormat(cleaned);
            format = cleaned;
        }
    }

    public void format(final StringBuffer target, final LogRecord lr) {
        new SimpleDateFormat(format).format(
            new Date(lr.getMillis()), target, new FieldPosition(0));
    }
}
