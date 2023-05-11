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

import org.scoja.server.core.EventContext;

public abstract class VariableHoleSkeleton
    extends HoleSkeleton {
    
    public VariableHoleSkeleton(final String name) {
        super(name);
    }
    
    public boolean isConstant() {
        return false;
    }
    
    protected void notConstantError() {
        throw new UnsupportedOperationException
            ("Hole " + this + " is not constant"
             + ": to be evaluated an environment is needed");
    }
    
    public void toFilename(final StringBuffer sb) {
        notConstantError();
    }
    
    protected void abusingTemplates(final StringBuffer sb,
                                    final EventContext ectx) {
        sb.append(getVarName()).append("_AT_FILENAME_IS_ABUSING_TEMPLATES");
    }
}
