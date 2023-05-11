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

package org.scoja.server.action;

import org.scoja.common.PriorityUtils;
import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;
import org.scoja.server.source.Internal;

public class EnvironmentSetUnknown extends ActionLinkableAtPython {

    protected final QStr value;

    public EnvironmentSetUnknown(final String value) {
        this.value = QStr.checked(value);
    }

    public void exec(final EventContext env) {
        env.getEnvironment().unknown(value);
    }
    
    public String toString() {
        return "to set unknown to value \"" +value+ "\"";
    }
}
