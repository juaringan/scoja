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
 * This is a <tt>confine</tt> function that forgets abruptly.
 */
public class SimpleConfineFunction extends String2StringFunction {
    
    protected final QStr confineResult;
    protected final int max;
    protected final long period;
    protected long nextPeriodStart;
    protected final Set<String> values;
    
    public SimpleConfineFunction(final StringExpression expr,
            final String confineResult, final int max, final long period) {
        this(expr, QStr.checked(confineResult), max, period);
    }
    
    public SimpleConfineFunction(final StringExpression expr,
            final QStr confineResult, final int max, final long period) {
        super(expr);
        if (period <= 0) throw new IllegalArgumentException(
            "Period must be positive");
        this.confineResult = confineResult;
        this.max = max;
        this.period = period;
        this.nextPeriodStart = 0;
        this.values = new HashSet<String>();
    }
    
    public StringExpression cloneClean() {
        return cloneCleanWith(subexpr1.cloneClean());
    }
    
    protected StringExpression cloneCleanWith(final StringExpression subclon) {
        return new SimpleConfineFunction(
            subclon, confineResult, max, period);
    }
    
    public QStr eval(final EventContext env) {
        final QStr qarg1 = super.eval(env);
        final String value = QStr.unqualified(qarg1);
        final long now = System.currentTimeMillis();
        synchronized (values) {
            if (nextPeriodStart <= now) {
                values.clear();
                nextPeriodStart = period * (1 + now/period);
            }
            if (values.contains(value)) return qarg1;
            if (max <= values.size()) return confineResult;
            values.add(value);
            return qarg1;
        }
    }    
}
