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

package org.scoja.server.core;

/**
 * An Environment with 2 scopes.
 * Look up is done first on the top scope, then on the other.
 * Modification and stacking is always done on the top scope.
 * @see HidingEnvironment for another way of joining environment.
 */
public class ScopedEnvironment implements Environment {

    protected final Environment top;
    protected final Environment bottom;
    
    public ScopedEnvironment(final Environment top, final Environment bottom) {
        this.top = top;
        this.bottom = bottom;
    }
    
    public ScopedEnvironment(final Environment bottom) {
        this(new StackedEnvironment(), bottom);
    }

    public void mark() { top.mark(); }
    
    public void release() { top.release(); }

    public boolean isDefined(final String var) {
        return top.isDefined(var) || bottom.isDefined(var);
    }

    public void define(final String var, final String value) {
        top.define(var, value);
    }
    
    public void define(final String var, final QStr value) {
        top.define(var, value);
    }
    
    public QStr definition(final String var) {
        QStr val = top.definition(var);
        if (val == null) val = bottom.definition(var);
        return val;
    }
    
    public void unknown(final String value) { top.unknown(value); }
    
    public void unknown(final QStr value) { top.unknown(value); }
    
    public QStr unknown() { return top.unknown(); }
}
