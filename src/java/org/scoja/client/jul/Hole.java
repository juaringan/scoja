/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser/Library General Public License
 * as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
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

package org.scoja.client.jul;

public abstract class Hole extends EventLayout {

    protected final String name;
    
    public Hole(final String name) {
        this.name = name;
    }
    
    public String getName() { return name; }

    public void with(final String args)
    throws IllegalArgumentException {
        withNoArguments(args);
    }
    
    protected void withNoArguments(final String args) {
        if (args != null && args.trim().length() != 0) {
            throw new IllegalArgumentException(name + " expects no argument");
        }
    }
}
