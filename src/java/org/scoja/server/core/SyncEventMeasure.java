/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2008  Bankinter, SA
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

import java.util.List;

/**
 * To accumulate the measure data of a set of events.
 * This is a synchronized version.
 * See {@link EventMeasure} for an unsynchronized version.
 */
public class SyncEventMeasure {
    protected long partialEvents, totalEvents;
    protected long partialBytes, totalBytes;
    
    public SyncEventMeasure() {
        this.partialEvents = this.totalEvents = 0;
        this.partialBytes = this.totalBytes = 0;
    }
    
    public void add(final EventContext ectx) {
        final int bs = ectx.getEvent().getByteSize();
        synchronized (this) {
            partialEvents++;
            partialBytes += bs;
        }
    }
    
    public void stats(final Measure.Key key, final List<Measure> measures) {
        stats(key, measures, false);
    }
    
    public void stats(final Measure.Key key, final List<Measure> measures,
            final boolean clearAfterReport) {
        final long pe, te, pb, tb;
        synchronized (this) {
            pe = partialEvents;  pb = partialBytes; 
            te = totalEvents + pe;  tb = totalBytes + pb;
            partialEvents = partialBytes = 0;
            if (clearAfterReport) totalEvents = totalBytes = 0;
            else { totalEvents = te;  totalBytes = tb; }
        }
        measures.add(new Measure(key, "events", pe, te));
        measures.add(new Measure(key, "event-bytes", pb, tb));
    }
}
