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

package org.scoja.server.template;

import java.io.PrintWriter;
import org.scoja.server.core.QStr;
import org.scoja.server.core.Environment;
import org.scoja.server.core.EventContext;
import org.scoja.server.expr.SecureFunction;

public class VarHole extends VariableHoleSkeleton {

    public VarHole(final String name) {
        super(name);
    }

    public void toFilename(final StringBuffer sb, final EventContext ectx) {
        final Environment env = ectx.getEnvironment();
        QStr value = env.definition(name);
        if (value == null) value = env.unknown();
        if (value.isFilenameSecure()) sb.append(value.unqualified());
        else sb.append(SecureFunction.secure(value.unqualified()));
    }
    
    public String textFor(final EventContext ectx) {
        final Environment env = ectx.getEnvironment();
        final QStr value = env.definition(name);
        return ((value == null) ? env.unknown() : value).unqualified();
    }
    
    //======================================================================
    public boolean equals(final Object other) {
        return (other instanceof VarHole)
            && equals((VarHole)other);
    }
    
    public boolean equals(final VarHole other) {
        return (other != null) && this.getVarName().equals(other.getVarName());
    }
    
    public int hashCode() {
        return getVarName().hashCode();
    }
}
