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
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import org.scoja.cc.text.escaping.Escaper;

public class Message extends EscapedHole {

    public Message() {
        this(null);
    }
    
    public Message(final Escaper escaper) {
        super("message", escaper);
    }
    
    public void with(final String args)
    throws IllegalArgumentException {
        installEscaper(args);
    }

    public void format(final StringBuffer target, final LogRecord lr) {
        final Object[] params = lr.getParameters();
        if (params == null) {
            appendEscaped(target, lr.getMessage());
        } else {
            final MessageFormat mf = new MessageFormat(lr.getMessage());
            if (escaper == null) {
                mf.format(params, target, new FieldPosition(0));
            } else {
                escaper.escape(mf.format(params), target);
            }
        }
    }
}
