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

package org.scoja.server.core;

/**
 * A {@link Link} that can be modified concurrenly.
 * So this link can be modified (target added or removed)
 * after the logging network it belongs to has been started.
 * {@link Link} already has a thread-safe implementation for adding or
 * removing a queue: it has no runtime cost.
 */
public class SyncLink extends Link {

    //FIXME: Change this to use reader/writer lock.
    protected final Object targetsLock;

    public SyncLink() {
        super();
        this.targetsLock = new Object();
    }
    
    public int getTargetSize() {
        synchronized (targetsLock) {
            return super.getTargetSize();
        }
    }
    
    public void removeTarget(final Linkable target) {
        synchronized (targetsLock) {
            super.removeTarget(target);
        }
    }
    
    public void addSimpleTarget(final Link target) {
        synchronized (targetsLock) {
            super.addSimpleTarget(target);
        }
    }
    
    public void propagate(final EventContext ectx) {
        synchronized (targetsLock) {
            super.propagate(ectx);
        }
    }
}
