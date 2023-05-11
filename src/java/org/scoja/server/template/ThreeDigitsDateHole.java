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
import java.util.Calendar;
import org.scoja.common.DateUtils;
import org.scoja.server.core.EventContext;

public class ThreeDigitsDateHole extends DateHoleSkeleton {

    protected final int key;
    protected final int shift;

    public ThreeDigitsDateHole(final String name, final int whichTimestamp,
                               final int key) {
        this(name, whichTimestamp, key, 0);
    }

    public ThreeDigitsDateHole(final String name, final int whichTimestamp,
                               final int key, final int shift) {
        super(name, whichTimestamp);
        this.key = key;
        this.shift = shift;
    }

    public void toFilename(final StringBuffer sb, final EventContext ectx) {
        try {
            DateUtils.append3Digits0(sb, get(ectx,key) + shift);
        } catch (java.io.IOException cannotHappen) {}
    }
    
    public void writeTo(final PrintWriter out, final EventContext ectx) {
        try {
            DateUtils.append3Digits0(out, get(ectx,key) + shift);
        } catch (java.io.IOException cannotHappen) {}
    }
}
