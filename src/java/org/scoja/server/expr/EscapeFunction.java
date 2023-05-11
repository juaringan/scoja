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

package org.scoja.server.expr;

import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;
import org.scoja.cc.text.escaping.Escaper;

public class EscapeFunction extends String2StringFunction {
    
    protected final Escaper esc;
    
    public EscapeFunction(final StringExpression subexpr, final Escaper esc) {
        super(subexpr);
        this.esc = esc;
    }

    protected StringExpression cloneCleanWith(final StringExpression subclon) {
        return new EscapeFunction(subclon, esc);
    }
        
    public QStr eval(final EventContext env) {
        final QStr qarg1 = super.eval(env);
        if (qarg1 == null) return null;
        final String arg1 = qarg1.unqualified();
        if (!esc.isAffected(arg1)) return qarg1;
        return QStr.unchecked(esc.escaped(arg1));
    }
}
