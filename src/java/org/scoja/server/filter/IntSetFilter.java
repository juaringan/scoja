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

package org.scoja.server.filter;

import java.util.BitSet;

import org.scoja.server.core.EventContext;

public abstract class IntSetFilter extends FilterLinkableAtPython {

    protected final BitSet valueSet;

    public IntSetFilter(final int value) {
        this.valueSet = new BitSet();
        this.valueSet.set(value);
    }
    
    public IntSetFilter(final int[] values) {
        this.valueSet = new BitSet();
        for (int i = 0; i < values.length; i++) {
            this.valueSet.set(values[i]);
        }
    }

    public boolean isGood(final int value) {
        return valueSet.get(value);
    }
    
    public abstract String getConceptName();
    
    public String toString() {
        return "testing whether " + getConceptName()
            + " is in set " + valueSet;
    }
}
