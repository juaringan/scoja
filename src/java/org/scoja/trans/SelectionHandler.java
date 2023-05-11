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

import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SelectableChannel;
import java.util.LinkedList;

public class SelectionHandler {

    protected final Selector selector;
    protected Selectable selectable;
    protected final LinkedList<InterestInterceptor> iis;
    protected Object attachment;
    protected SelectionKey key;
    protected boolean enabled;
    protected int ops;
    
    public SelectionHandler(final Selector selector,
            final Object attachment) {
        this.selector = selector;
        this.selectable = null;
        this.iis = new LinkedList<InterestInterceptor>();
        this.attachment = attachment;
        this.key = null;
        this.enabled = true;
        this.ops = 0;
    }

    //======================================================================
    // For Transport users.
    
    public void disable() {
        enable(false);
    }
    
    public void enable() {
        enable(true);
    }
    
    public void enable(final boolean newEnabled) {
        if (newEnabled != enabled) {
            enabled = newEnabled;
            updateInterestOps();
        }
    }
        
    public void interestOps(final int ops) {
        if (this.ops != ops) {
            this.ops = ops;
            if (enabled) updateInterestOps();
        }
    }
    
    public Object attachment() { return attachment; }
    public void attach(final Object attachment) {
        this.attachment = attachment;
    }
    
    public Selector selector() { return selector; }
    
    public Selectable selectable() { return selectable; }
    
    public SelectionKey key() { return key; }
    
    
    //======================================================================
    // For Transport implementors.
    
    public void addSelectable(final Selectable selectable) {
        if (this.selectable == null) this.selectable = selectable;
    }
    
    public void addInterestInterceptor(final InterestInterceptor ii) {
        this.iis.add(ii);
    }
    
    public void register(final SelectableChannel channel) {
        try {
            key = channel.register(selector, effectiveInterestOps(), this);
        } catch (ClosedChannelException ignored) {
            //An asynchronous close
        }
    }
    
    public void updateInterestOps() {
        if (key != null) key.interestOps(effectiveInterestOps());
    }
    
    protected int effectiveInterestOps() {
        int eops;
        if (enabled) {
            eops = ops;
            for (final InterestInterceptor ii: iis)
                eops = ii.interestOps(eops);
        } else {
            eops = 0;
        }
        //System.err.println("EFFECTIVE INTEREST: " + eops);
        return eops;
    }
}
