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
package org.scoja.trans;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.security.Principal;

public interface RemoteInfo {

    public InetAddress inetAddress();
    
    public Principal principal();
    
    
    //======================================================================
    public static abstract class Skeleton implements RemoteInfo {
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Remote[addr: ").append(inetAddress());
            if (principal() != null)
                sb.append(", principal: ").append(principal());
            return sb.append(']').toString();
        }
    }
    
    
    //======================================================================
    public static class Proxy extends Skeleton {
        protected final RemoteInfo base;
        
        public Proxy(final RemoteInfo base) {
            this.base = base;
        }
        
        public InetAddress inetAddress() { return base.inetAddress(); }
        
        public Principal principal() { return base.principal(); }
    }
    
    
    //======================================================================
    public static class Inet extends Skeleton {
        
        protected final InetAddress address;
        
        public Inet(final InetAddress address) {
            this.address = address;
        }
        
        public static Inet from(final SocketChannel s) {
            InetAddress address = null; {
                if (s != null) {
                    final SocketAddress sa
                        = s.socket().getRemoteSocketAddress();
                    if (sa instanceof InetSocketAddress) 
                        address = ((InetSocketAddress)sa).getAddress();
                }
            }
            return new Inet(address);
        }
        
        public InetAddress inetAddress() { return address; }
        
        public Principal principal() { return null; }
    }
}
