/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2007  Bankinter
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

import org.scoja.cc.lang.XMath;
import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;

/**
 */
public class HashingFunction extends String2StringFunction {
    
    protected final int mod;
    protected final int digits;
    
    public HashingFunction(final StringExpression subexpr,
                           final int mod) {
        super(subexpr);
        this.mod = mod;
        this.digits = XMath.log(10, mod - 1) + 1;
    }
    
    protected StringExpression cloneCleanWith(final StringExpression subclon) {
        return new HashingFunction(subclon, mod);
    }
    
    public QStr eval(final EventContext env) {
        final QStr qarg1 = super.eval(env);
        int h = (qarg1.unqualified().hashCode() & Integer.MAX_VALUE) % mod;
        final char[] cs = new char[digits];
        int i = cs.length;
        while (h > 0) { cs[--i] = (char)('0' + h%10); h /= 10; }
        while (i > 0) cs[--i] = '0';
        return QStr.supposePerfect(new String(cs));
    }
}
