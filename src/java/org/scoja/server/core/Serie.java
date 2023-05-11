/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
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

public interface Serie {
    
    public double next();
    
    
    //======================================================================
    public static class Exp implements Serie {
        protected double next;
        protected final double base;
        
        public Exp(final double init, final double base) {
            this.next = init;
            this.base = base;
        }
        
        public double next() {
            final double result = next;
            next *= base;
            return result;
        }
    }
    
    
    //======================================================================
    public static class Bounded implements Serie {
        protected final Serie source;
        protected final double max;
        
        public Bounded(final double max, final Serie source) {
            this.source = source;
            this.max = max;
        }
        
        public double next() {
            return Math.min(source.next(), max);
        }
    }
}
