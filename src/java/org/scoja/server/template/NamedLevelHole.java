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

package org.scoja.server.template;

import java.io.PrintWriter;
import org.scoja.server.core.EventContext;
import org.scoja.common.PriorityUtils;

public class NamedLevelHole extends VariableHoleSkeleton {

    public NamedLevelHole(final String name) {
        super(name);
    }

    public void toFilename(final StringBuffer sb, final EventContext ectx) {
        sb.append(getLevelName(ectx));
    }

    public String textFor(final EventContext ectx) {
        return getLevelName(ectx);
    }
    
    protected String getLevelName(final EventContext ectx) {
        return ectx.getEvent().getLevelName();
    }

    public String getVarName() { return "LEVEL"; }
}
