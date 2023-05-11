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

import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;

public class UpCased extends String2StringFunction {
    
    public UpCased(final StringExpression subexpr) {
        super(subexpr);
    }
    
    protected StringExpression cloneCleanWith(final StringExpression subclon) {
        return new UpCased(subclon);
    }
    
    public QStr eval(final EventContext ectx) {
        final QStr val = super.eval(ectx);
        if (val == null) return null;
        return QStr.unchecked(val.unqualified().toUpperCase());
    }
}
