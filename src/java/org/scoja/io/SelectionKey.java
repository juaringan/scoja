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

package org.scoja.io;

public class SelectionKey {

    public static final int OP_ACCEPT = 1;
    public static final int OP_READ = 1;
    public static final int OP_CONNECT = 2;
    public static final int OP_WRITE = 2;
    
    protected final Selector selector;
    protected final SelectableChannel channel;
    protected int currentInterest;
    protected int currentReady;
    protected boolean canceled;
    protected Object attribute;
    
    protected SelectionKey(final Selector selector,
                           final SelectableChannel channel,
                           final int interest) {
        this.selector = selector;
        this.channel = channel;
        this.currentInterest = interest;
        this.currentReady = 0;
        this.canceled = false;
        this.attribute = null;
    }
    
    public Object attach(final Object attribute) {
        final Object previous = this.attribute;
        this.attribute = attribute;
        return previous;
    }
    
    public Object attachment() {
        return attribute;
    }
    
    protected void checkValid(final boolean wanted) {
        if (canceled == wanted) {
            throw new IllegalStateException
                ("This selection key has been canceled");
        }
    }
    
    public synchronized Selector selector() {
        checkValid(true);
        return selector;
    }
    
    public synchronized SelectableChannel channel() {
        checkValid(true);
        return channel;
    }
    
    public void cancel() {
        selector.cancelKey(this);
        synchronized (this) {
            canceled = true;
        }
    }
    
    public synchronized boolean isValid() {
        return !canceled;
    }
    
    public synchronized int interestOps() {
        checkValid(true);
        return currentInterest;
    }
    
    public synchronized SelectionKey interestOps(final int newInterest) {
        checkValid(true);
        if (currentInterest != newInterest) {
            //selector.interestChanging(this, currentInterest, newInterest);
            currentInterest = newInterest;
        }
        return this;
    }

    protected void readyOps(final int newReady) {
        this.currentReady = newReady;
    }
    
    public synchronized int readyOps() {
        checkValid(true);
        return currentReady;
    }
        
    protected boolean is(final int what) {
        return (readyOps() | what) != 0;
    }
    
    public boolean isAcceptable() {
        return is(OP_ACCEPT);
    }
    
    public boolean isConnectable() {
        return is(OP_CONNECT);
    }
    
    public boolean isReadable() {
        return is(OP_READ);
    }
    
    public boolean isWritable() {
        return is(OP_WRITE);
    }
    
    //======================================================================
    public synchronized String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName()).append('[');
        if (canceled) {
            sb.append("canceled");
        } else {
            sb.append("channel: ").append(channel)
                .append(", interest: ").append(currentInterest)
                .append(", ready: ").append(currentReady)
                .append(", attachment: ").append(attribute);
        }
        sb.append(']');
        return sb.toString();
    }
}
