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

import java.util.Map;
import java.util.HashMap;

import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;

public class CustomizerFunction extends String2StringFunction {
    
    protected final long period;
    protected final StringExpression fun;
    protected final Map<String,StringExpression> cases;
    protected long nextPeriodStart;
    
    public CustomizerFunction(final StringExpression selector,
            final long period, final StringExpression fun) {
        super(selector);
        this.period = period;
        this.fun = fun;
        this.cases = new HashMap<String,StringExpression>();
        this.nextPeriodStart = 0;
    }
    
    public StringExpression cloneCleanWith(final StringExpression subclon) {
        return new CustomizerFunction(subclon, period, fun);
    }
    
    public QStr eval(final EventContext env) {
        final String key = QStr.unqualified(super.eval(env));
        StringExpression cfun;
        final long now = (period <= 0) ? -1 : System.currentTimeMillis();
        synchronized (cases) {
            if (nextPeriodStart <= now) {
                cases.clear();
                nextPeriodStart = period * (1 + now/period);
            }
            cfun = cases.get(key);
            if (cfun == null) {
                cfun = fun.cloneClean();
                cases.put(key, cfun);
            }
        }
        return cfun.eval(env);
    }

    public String toString() {
        return "customized [" + fun + "]";
    }
}
