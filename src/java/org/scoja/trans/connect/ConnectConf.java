/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2010  Mario Martínez
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
package org.scoja.trans.connect;

import org.scoja.cc.lang.Maybe;

public interface ConnectConf {

    public Maybe<Boolean> getKeepAlive();
    public void setKeepAlive(boolean enabled);
    
    public Maybe<String> getUserAgent();
    public void setUserAgent(String valued);

        
    //======================================================================
    public static abstract class Skeleton implements ConnectConf {
        public String toString() {
            return "ConnectConf["
                + "keep alive: " + getKeepAlive()
                + ", user agent: " + getUserAgent()
                + "]";
        }
    }
    
    
    //======================================================================
    public static class Stacked extends Skeleton {
        
        protected final Maybe.Mutable<Boolean> keepAlive;
        protected final Maybe.Mutable<String> userAgent;
        
        public Stacked() {
            this(null);
        }
        
        public Stacked(final ConnectConf stack) {
            if (stack == null) {
                this.keepAlive = Maybe.Stacked.undef();
                this.userAgent = Maybe.Stacked.undef();
            } else {
                this.keepAlive = stack.getKeepAlive().push();
                this.userAgent = stack.getUserAgent().push();
            }
        }
        
        public Maybe<Boolean> getKeepAlive() { return keepAlive; }
        public void setKeepAlive(final boolean enabled) {
            keepAlive.set(enabled);
        }
    
        public Maybe<String> getUserAgent() { return userAgent; }
        public void setUserAgent(final String value) {
            userAgent.set(value);
        }
    }
}
