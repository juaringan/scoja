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

package org.scoja.popu.common;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;

/**
 * An Event is an entry in a log file.
 * This class is mutable, so it is necessary to copy an Event before to
 * store it.
 */
public class Event
    implements Cloneable {

    protected char[] data;
    protected int init;
    protected int end;
    protected long timestamp;
    protected int tsinit;
    protected int tsend;
    protected boolean tsignore;
    
    /**
     * Deep copy constructor.
     */
    public Event(final Event other) {
        final int l = end-init;
        final char[] data = new char[l];
        System.arraycopy(other.data,init, data,0,l);
        
        this.data = data;
        this.init = 0;
        this.end = l;
        this.timestamp = other.timestamp;
        this.tsinit = other.tsinit - other.init;
        this.tsend = other.tsend - other.init;
        this.tsignore = other.tsignore;
    }
    
    public Event(final char[] data) {
        this(data, 0, 0, Long.MIN_VALUE, 0, 0);
    }
    
    public Event(final char[] data, final int init, final int end,
                 final long timestamp, final int tsinit, final int tsend) {
        this.data = data;
        this.init = init;
        this.end = end;
        this.timestamp = timestamp;
        this.tsinit = tsinit;
        this.tsend = tsend;
        this.tsignore = false;
    }
    
    public Event with(final char[] data) {
        this.data = data;
        return this;
    }
    
    public Event with(final long timestamp,
                      final int tsinit, final int tsend) {
        this.timestamp = timestamp;
        this.tsinit = tsinit;
        this.tsend = tsend;
        return this;
    }
    
    public Event with(final int init, final int end) {
        this.init = init;
        this.end = end;
        return this;
    }
    
    public Event ignoreTimestamp(final boolean tsignore) {
        this.tsignore = tsignore;
        return this;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void writeTo(final Writer out)
    throws IOException {
        out.write(data, init, end-init);
    }
    
    public String toString() {
        return "Event[stamp: " + new Date(timestamp)
            + ", data: " + new String(data, init, end-init)
            + "]";
    }
    
    public Object clone() {
        return new Event(this);
    }
    
    public int hashCode() {
        return tsignore
            ? hashCode(hashCode(0, data,init,tsinit), data,tsend,end)
            : hashCode(0, data,init,end);
    }

    protected static int hashCode(
        final int ih,
        final char[] data, final int init, final int end) {
        int h = ih;
        for (int i = init; i < end; i++) {
            h = (h << 4) + data[i];
            final int u = h & 0xF0000000;
            if (u != 0) h = (h ^ (u >>> 24)) & 0x0FFFFFFF;
        }
        return h;
    }
    
    public boolean equals(final Object other) {
        return (other instanceof Event)
            && equals((Event)other);
    }
    
    public boolean equals(final Event other) {
        return other != null
            && equals(this, other, this.tsignore || other.tsignore);
    }
    
    public static boolean equals(final Event e1, final Event e2,
                                 final boolean tsignore) {
        if ((e1 == null) != (e2 == null)) return false;
        if (e1 == null) return true;
        if (tsignore) {
            return equals(e1.data, e1.init, e1.tsinit,
                          e2.data, e2.init, e2.tsinit)
                && equals(e1.data, e1.tsend, e1.end,
                          e2.data, e2.tsend, e2.end);
        } else {
            return e1.timestamp == e2.timestamp
                && equals(e1.data, e1.init, e1.end,
                          e2.data, e2.init, e2.end);
        }
    }
        
    protected static boolean equals(
        final char[] c1, final int i1, final int e1,
        final char[] c2, final int i2, final int e2) {
        final int l = e1 - i1;
        if (l != e2-i2) return false;
        for (int i = 0; i < l; i++) {
            if (c1[i1+i] != c2[i2+i]) return false;
        }
        return true;
    }
}
