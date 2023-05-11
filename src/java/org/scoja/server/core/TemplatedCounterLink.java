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

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * A {@link Link} that takes some statistics from the events that it sees.
 * It keeps several statistics depending on event data.
 * It uses templates to produce the stat keys.
 * @see CounterLink for a version with only one stat.
 *
 * @implementation
 * The map {@link #key2measure} contains all the measures.
 * It is updated with {@link #lock} locked, both the modification to the map
 * structured and the to the {@link EventMeasure} entries.
 * The second one could be done in its own critical region, for instance, with
 * a {@link SyncEventMeasure}.
 * But it is such a short work compared to get it from the map, that it cannot
 * harm concurrency.
 * Doing it this way, the method {@link #stat} can be implemented such that
 * there is almost no big lock: the map is substituted for an empty one and
 * later updated if <tt>!{@link #clearAfterReport}</tt>.
 * It is not enough to copy the map because EventMeasures cannot be shared
 * while reporting.
 * The only big lock is the update, but it will not happen in the normal case
 * when {@link #clearAfterReport} is true.
 */
public class TemplatedCounterLink extends Link implements Measurable {

    protected final Measure.TemplatedKey tkey;
    protected final Object lock;
    protected Map<Measure.Key, EventMeasure> key2measure;
    protected boolean clearAfterReport;

    public TemplatedCounterLink(final Measure.TemplatedKey tkey) {
        super();
        this.tkey = tkey;
        this.lock = new Object();
        this.key2measure = new HashMap<Measure.Key, EventMeasure>();
        this.clearAfterReport = false;
    }
    
    public void setClearAfterReport(final boolean enabled) {
        this.clearAfterReport = enabled;
    }

    public void process(final EventContext ectx) {
        final Measure.Key key = tkey.key(ectx);
        synchronized (lock) {
            EventMeasure measure = key2measure.get(key);
            if (measure == null) {
                measure = new EventMeasure();
                key2measure.put(key, measure);
            }
            measure.add(ectx);
        }
        super.process(ectx);
    }
    
    public Measure.Key getMeasureKey() {
        throw new UnsupportedOperationException("This is a templated counter");
    }
    
    public void stats(final List<Measure> measures) {
        final Map<Measure.Key,EventMeasure> k2m;
        synchronized (lock) {
            k2m = key2measure;
            key2measure = new HashMap<Measure.Key,EventMeasure>(k2m.size());
        }
        for (final Map.Entry<Measure.Key,EventMeasure> entry: k2m.entrySet()) {
            entry.getValue().stats(entry.getKey(), measures);
        }
        if (clearAfterReport) return;
        synchronized (lock) {
            for (final Map.Entry<Measure.Key,EventMeasure> entry
                     : k2m.entrySet()) {
                final Measure.Key key = entry.getKey();
                final EventMeasure oldmea = entry.getValue();
                final EventMeasure newmea = key2measure.get(key);
                if (newmea == null) key2measure.put(key, oldmea);
                else newmea.add(oldmea);
            }
        }
    }
}
