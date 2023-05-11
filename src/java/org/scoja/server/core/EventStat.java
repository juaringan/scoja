/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2011  Bankinter, S.A.
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

public class EventStat {

    protected long count;
    protected long size;
    protected boolean blocked;
    
    public EventStat() {
        this.count = 0;
        this.size = 0;
        this.blocked = false;
    }
    
    public boolean isBlocked() { return blocked; }
    
    public void block() { this.blocked = true; }
    
    public void considerEvent(final Event event) {
        considerEvent(event.getByteSize());
    }
    
    public void considerEvent(final int size) {
        this.count++;
        this.size += size;
    }
    
    public boolean reachesCount(final long maxCount) {
        return maxCount > 0 && count >= maxCount;
    }
    
    public boolean reachesSize(final long maxSize) {
        return maxSize > 0 && size >= maxSize;
    }
    
    public String toString() {
        return "EventStat[count: " + count + ", size: " + size + "]";
    }
}
