/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2007  Mario Martínez
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

/**
 */
public class StringMappingFunction extends String2StringFunction {
    
    protected final Map map;
    protected QStr defaultResult;
    
    public StringMappingFunction(final StringExpression subexpr,
                                 final String[] keys,
                                 final String[] values) {
        super(subexpr);
        this.defaultResult = null;
        if (keys.length != values.length) {
            throw new IllegalArgumentException
                ("Number of keys (" +keys.length+ ") differs from"
                 + " number of values (" +values.length+ ")");
        }
        this.map = new HashMap(keys.length);
        for (int i = 0; i < keys.length; i++) {
            this.map.put(keys[i], QStr.checked(values[i]));
        }
    }
    
    public void setDefault(final String defaultResult) {
        this.defaultResult = QStr.checked(defaultResult);
    }
    
    public StringMappingFunction(final StringExpression subexpr,
            final StringMappingFunction other) {
        super(subexpr);
        this.map = other.map;
        this.defaultResult = other.defaultResult;
    }
    
    protected StringExpression cloneCleanWith(final StringExpression subclon) {
        return new StringMappingFunction(subclon, this);
    }
    
    public QStr eval(final EventContext env) {
        final QStr qarg1 = super.eval(env);
        final String arg1 = qarg1.unqualified();
        final QStr result = (QStr)map.get(arg1);
        if (result != null) return result;
        else if (defaultResult != null) return defaultResult;
        else return qarg1;
    }
}
