/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003  Mario Mart�nez
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
import org.scoja.server.core.Environment;
import org.scoja.server.core.EventContext;

public class EnvironmentGet extends StringExpressionAtPython {
    
    protected final String varName;
    
    public EnvironmentGet(final String varName) {
        this.varName = varName;
    }
    
    public QStr eval(final EventContext env) {
        final QStr value = env.getEnvironment().definition(varName);
        return (value != null) ? value : Environment.Q_UNKNOWN;
    }
}
