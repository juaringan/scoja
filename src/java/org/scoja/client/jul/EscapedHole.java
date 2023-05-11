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

import org.scoja.cc.text.escaping.CLike;
import org.scoja.cc.text.escaping.Escaper;
import org.scoja.cc.text.escaping.EscaperCache;

public abstract class EscapedHole extends Hole {

    protected Escaper escaper;
    
    public EscapedHole(final String name) {
        this(name, null);
    }
    
    public EscapedHole(final String name, final Escaper escaper) {
        super(name);
        this.escaper = escaper;
    }

    protected void installEscaper(final String escdef)
    throws IllegalArgumentException {
        final int methodColon = escdef.indexOf(':');
        final String method;
        String arguments;
        if (methodColon == -1) {
            method = escdef.trim();
            arguments = null;
        } else {
            method = escdef.substring(0,methodColon).trim();
            arguments = escdef.substring(methodColon+1);
        }
        if (method.length() == 0 && arguments == null) {
            escaper = null;
            return;
        }
        if (arguments == null) {
            arguments = Escaper.Avoid.CONTROL_SEQUENCES;
        } else {
            final int argsColon = arguments.indexOf(':');
            if (argsColon != -1) {
                arguments = arguments.substring(0,argsColon);
            }
            arguments = CLike.noControlSequence().unescaped(arguments);
        }
        escaper = EscaperCache.getMainInstance().escaperFor(method, arguments);
    }
    
    protected void appendEscaped(final StringBuffer target, final String data){
        if (escaper == null) target.append(data);
        else escaper.escape(data, target);
    }
}
