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
import java.util.Collection;

public class MixingEventSource
    implements EventSource {
    
    protected final EventSource[] sources;
    protected int inUse;
    protected int min;
    protected long lastStamp;
    
    public MixingEventSource(final EventSource[] sources) {
        this.sources = sources;
        this.inUse = sources.length;
        this.min = -1;
        this.lastStamp = Long.MIN_VALUE;
    }
    
    public MixingEventSource(final Collection/*<EventSource>*/ sources) {
        this((EventSource[])sources.toArray(new EventSource[0]));
    }

    public void advance()
    throws IOException {
        if (min == -1) {
            for (int i = 0; i < inUse; ) {
                if (advance(i)) i++;
            }
            min = 0;
        } else {
            advance(min);
            if (min >= inUse) min = inUse-1;
        }
        if (inUse == 0) return;
        if (sources[min].current().getTimestamp() == lastStamp) return;
        min = 0;
        lastStamp = sources[0].current().getTimestamp();
        for (int i = 1; i < inUse; i++) {
            final long candidateStamp = sources[i].current().getTimestamp();
            if (candidateStamp < lastStamp) {
                min = i;
                lastStamp = candidateStamp;
            }
        }
    }
    
    protected boolean advance(final int i)
    throws IOException {
        sources[i].advance();
        if (sources[i].has()) {
            return true;
        } else {
            inUse--;
            final EventSource tmp = sources[inUse];
            sources[inUse] = sources[i];
            sources[i] = tmp;
            return false;
        }
    }
    
    public boolean has() {
        return inUse > 0;
    }
    
    public Event current() {
        return sources[min].current();
    }
}
