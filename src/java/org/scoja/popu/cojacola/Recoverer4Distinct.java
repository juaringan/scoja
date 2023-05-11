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
package org.scoja.popu.cojacola;

import java.io.IOException;

import org.scoja.popu.common.Event;
import org.scoja.popu.common.EventSource;
import org.scoja.popu.common.WindowedEventSource;

public class Recoverer4Distinct implements EventSource {

    protected final WindowedEventSource[] sources;
    protected int prev;
    protected Event event;
    
    public Recoverer4Distinct(final WindowedEventSource main,
                              final WindowedEventSource secondary) {
        this.sources = new WindowedEventSource[] {main, secondary};
        this.prev = -1;
        this.event = null;
    }

    public void advance()
    throws IOException {
        if (prev == -1) {
            sources[0].advance();
            sources[1].advance();
        } else {
            sources[prev].advance();
        }
    }
    
    public boolean has() {
        return event != null;
    }
    
    public Event current() {
        return event;
    }
}
