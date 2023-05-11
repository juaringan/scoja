/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2012  LogTrust
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

import java.util.Set;
import java.util.HashSet;

import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;

/**
 * This is a <tt>confine</tt> function that never forgots.
 */
public class EternalConfineFunction extends String2StringFunction {
    
    protected final QStr confineResult;
    protected final int max;
    protected final Set<String> values;
    
    public EternalConfineFunction(final StringExpression expr,
            final String confineResult, final int max) {
        this(expr, QStr.checked(confineResult), max);
    }
    
    public EternalConfineFunction(final StringExpression expr,
            final QStr confineResult, final int max) {
        super(expr);
        this.confineResult = confineResult;
        this.max = max;
        this.values = new HashSet<String>();
    }
    
    public StringExpression cloneClean() {
        return cloneCleanWith(subexpr1.cloneClean());
    }
    
    protected StringExpression cloneCleanWith(final StringExpression subclon) {
        return new EternalConfineFunction(
            subclon, confineResult, max);
    }
    
    public QStr eval(final EventContext env) {
        final QStr qarg1 = super.eval(env);
        final String value = QStr.unqualified(qarg1);
        synchronized (values) {
            if (values.contains(value)) return qarg1;
            if (max <= values.size()) return confineResult;
            values.add(value);
            return qarg1;
        }
    }    
}
