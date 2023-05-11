/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2008  Bankinter, S.A.
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

package org.scoja.server.cache;

public interface SpaceController {
    
    public int resizing(int current, int desired);
    
    public void resizeFailed(int allowed, int old);
    
    
    //======================================================================
    public static class Unlimited implements SpaceController {
        public int resizing(final int current, final int desired) {
            return desired;
        }
        
        public void resizeFailed(final int allowed, final int old) {}
    }
    
    
    //======================================================================
    public static class HardLimited implements SpaceController {
        protected final int totalMax;
        protected final int perEntityMax;
        protected int totalAvailable;
        
        public HardLimited(final int totalMax, final int perEntityMax) {
            this.totalMax = totalMax;
            this.perEntityMax = perEntityMax;
            this.totalAvailable = totalMax;
        }
         
        public int resizing(final int current, final int desired) {
            int inc;
            if (desired <= current) {
                inc = desired - current;
                synchronized (this) {
                    totalAvailable -= inc;
                }
            } else {
                inc = (perEntityMax <= 0)
                    ? (desired - current)
                    : Math.max(0, Math.min(desired,perEntityMax) - current);
                synchronized (this) {
                    inc = Math.min(inc, totalAvailable);
                    totalAvailable -= inc;
                }
            }
            return current + inc;
        }
        
        public void resizeFailed(final int allowed, final int old) {
            synchronized (this) {
                totalAvailable += allowed - old;
            }
        }
    }
}
