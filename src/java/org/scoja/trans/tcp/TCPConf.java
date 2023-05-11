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
package org.scoja.trans.tcp;

import java.io.IOException;
import org.scoja.cc.lang.Maybe;

public interface TCPConf {
    
    public Maybe<Integer> getTimeout();
    public void setTimeout(int timeout)
    throws IOException;

    public Maybe<Boolean> getReuseAddress();    
    public void setReuseAddress(boolean enabled)
    throws IOException;
    
    public Maybe<Integer> getSendBufferSize();
    public void setSendBufferSize(int size)
    throws IOException;
    
    public Maybe<Integer> getReceiveBufferSize();
    public void setReceiveBufferSize(int size)
    throws IOException;
    
    public Maybe<Boolean> getKeepAlive();
    public void setKeepAlive(boolean enabled)
    throws IOException;
    
    public Maybe<Boolean> getNoDelay();
    public void setNoDelay(boolean enabled)
    throws IOException;
    
    public Maybe<Integer> getLinger();
    public void setLinger(int linger)
    throws IOException;
    
    public static final int LOWCOST = 0x02;
    public static final int RELIABILITY = 0x04;
    public static final int THROUGHPUT = 0x08;
    public static final int LOWDELAY = 0x10;

    public Maybe<Integer> getTrafficClass();
    public void setTrafficClass(int traffic)
    throws IOException;

    
    //======================================================================
    public static abstract class Skeleton implements TCPConf {
        public String toString() {
            return "TCPConf["
                + "timeout: " + getTimeout()
                + ", reuse address: " + getReuseAddress()
                + ", send buffer size: " + getSendBufferSize()
                + ", receive buffer size: " + getReceiveBufferSize()
                + ", keep alive: " + getKeepAlive()
                + ", no delay: " + getLinger()
                + ", traffic class: " + getTrafficClass()
                + "]";
        }
    }
    
        
    //======================================================================
    public static class Proxy extends Skeleton {
        protected final TCPConf base;
        
        public Proxy(final TCPConf base) {
            this.base = base;
        }
        
        public Maybe<Integer> getTimeout() {
            return base.getTimeout(); }
        public void setTimeout(final int timeout)
        throws IOException {
            base.setTimeout(timeout); }
        
        public Maybe<Boolean> getReuseAddress() {
            return base.getReuseAddress(); }
        public void setReuseAddress(final boolean enabled)
        throws IOException {
            base.setReuseAddress(enabled); }
    
        public Maybe<Integer> getSendBufferSize() {
            return base.getSendBufferSize(); }
        public void setSendBufferSize(final int size)
        throws IOException {
            base.setSendBufferSize(size); }
    
        public Maybe<Integer> getReceiveBufferSize() {
            return base.getReceiveBufferSize(); }
        public void setReceiveBufferSize(final int size)
        throws IOException {
            base.setReceiveBufferSize(size); }
    
        public Maybe<Boolean> getKeepAlive() {
            return base.getKeepAlive(); }
        public void setKeepAlive(final boolean enabled)
        throws IOException {
            base.setKeepAlive(enabled); }
    
        public Maybe<Boolean> getNoDelay() {
            return base.getNoDelay(); }
        public void setNoDelay(final boolean enabled)
        throws IOException {
            base.setNoDelay(enabled); }
    
        public Maybe<Integer> getLinger() {
            return base.getLinger(); }
        public void setLinger(final int linger)
        throws IOException {
            base.setLinger(linger); }
    
        public Maybe<Integer> getTrafficClass() {
            return base.getTrafficClass(); }
        public void setTrafficClass(final int traffic)
        throws IOException {
            base.setTrafficClass(traffic); }
    }
    
        
    //======================================================================
    public static class Stacked extends Skeleton {
    
        protected final Maybe.Mutable<Integer> timeout;
        protected final Maybe.Mutable<Boolean> reuseAddress;
        protected final Maybe.Mutable<Integer> sendBufferSize;
        protected final Maybe.Mutable<Integer> receiveBufferSize;
        protected final Maybe.Mutable<Boolean> keepAlive;
        protected final Maybe.Mutable<Boolean> noDelay;
        protected final Maybe.Mutable<Integer> linger;
        protected final Maybe.Mutable<Integer> trafficClass;
        
        public Stacked() {
            this(null);
        }
        
        public Stacked(final TCPConf base) {
            if (base == null) {
                this.timeout = Maybe.Stacked.undef();
                this.reuseAddress = Maybe.Stacked.undef();
                this.sendBufferSize = Maybe.Stacked.undef();
                this.receiveBufferSize = Maybe.Stacked.undef();
                this.keepAlive = Maybe.Stacked.undef();
                this.noDelay = Maybe.Stacked.undef();
                this.linger = Maybe.Stacked.undef();
                this.trafficClass = Maybe.Stacked.undef();
            } else {
                this.timeout = base.getTimeout().push();
                this.reuseAddress = base.getReuseAddress().push();
                this.sendBufferSize = base.getSendBufferSize().push();
                this.receiveBufferSize = base.getReceiveBufferSize().push();
                this.keepAlive = base.getKeepAlive().push();
                this.noDelay = base.getNoDelay().push();
                this.linger = base.getLinger().push();
                this.trafficClass = base.getTrafficClass().push();
            }
        }
    
        public Maybe<Integer> getTimeout() { return timeout; }
        public void setTimeout(final int time)
        throws IOException {
            timeout.set(time);
        }
    
        public Maybe<Boolean> getReuseAddress() { return reuseAddress; }
        public void setReuseAddress(final boolean enabled)
        throws IOException {
            reuseAddress.set(enabled);
        }
        
        public Maybe<Integer> getSendBufferSize() { return sendBufferSize; }
        public void setSendBufferSize(final int size)
        throws IOException {
            sendBufferSize.set(size);
        }
        
        public Maybe<Integer> getReceiveBufferSize(){return receiveBufferSize;}
        public void setReceiveBufferSize(final int size)
        throws IOException {
            receiveBufferSize.set(size);
        }
        
        public Maybe<Boolean> getKeepAlive() { return keepAlive; }
        public void setKeepAlive(final boolean enabled)
        throws IOException {
            keepAlive.set(enabled);
        }
        
        public Maybe<Boolean> getNoDelay() { return noDelay; }
        public void setNoDelay(final boolean enabled)
        throws IOException {
            noDelay.set(enabled);
        }
        
        public Maybe<Integer> getLinger() { return linger; }
        public void setLinger(final int time)
        throws IOException {
            linger.set(time);
        }
        
        public Maybe<Integer> getTrafficClass() { return trafficClass; }
        public void setTrafficClass(final int traffic)
        throws IOException {
            trafficClass.set(traffic);
        }
    }
}
