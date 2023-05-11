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

package org.scoja.popu.common;

public interface Locator
    extends Cloneable {

    public void locate(char[] data, int init, int end);
    
    public boolean located();
    
    public int init();
    
    public int end();
    
    public void reset();
    
    public Object clone();
    
    public String toPattern();
    
    
    //======================================================================
    public static abstract class Skeleton
        implements Locator {
        
        protected boolean located;
        protected int init;
        protected int end;
        
        public Skeleton() {
            this.located = false;
            this.init = 0;
            this.end = 0;
        }

        public boolean located() { return located; }
        public int init() { return init; }
        public int end() { return end; }        
        
        public void reset() { located = false; }
        
        public abstract Object clone();
    }
}
