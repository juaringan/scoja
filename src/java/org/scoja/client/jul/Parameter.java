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

import java.util.logging.LogRecord;

import java.io.PrintWriter;
import org.scoja.io.StringBufferWriter;

public class Parameter extends EscapedHole {
    
    protected int n;
    
    public Parameter() {
        super("parameter");
        n = 0;
    }
    
    public void with(final String args)
    throws IllegalArgumentException {
        final String idxStr, escdesc;
        final int colon = args.indexOf(':');
        if (colon == -1) {
            idxStr = args;
            escdesc = null;
        } else {
            idxStr = args.substring(0,colon);
            escdesc = args.substring(colon+1);
        }
        final int idx = Integer.parseInt(idxStr);
        if (escdesc != null) {
            installEscaper(escdesc);
        }
        this.n = idx;
    }

    public void format(final StringBuffer target, final LogRecord lr) {
        final Object[] params = lr.getParameters();
        if (params == null) return;
        final int m = (n >= 0) ? n : (params.length+n);
        if (m < 0 || m >= params.length) return;
        appendEscaped(target, params[m].toString());
    }
}
