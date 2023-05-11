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

package org.scoja.popu.common;

public class JumpLocator
    extends Locator.Skeleton {

    protected final int jump;
    
    public JumpLocator(final int jump) {
        this.jump = jump;
    }
    
    public JumpLocator(final JumpLocator other) {
        this.jump = other.jump;
    }
    
    public void locate(final char[] data, final int init, final int end) {
        if (end - init >= jump) {
            this.located = true;
            this.init = this.end = init+jump;
        } else {
            this.init = init;
        }
    }
    
    public Object clone() {
        return new JumpLocator(this);
    }
    
    public String toPattern() { 
        return "->" + jump;
    }
}
