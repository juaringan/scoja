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

/**
 * A {@link Link} that takes some statistics from the events that it sees.
 * All statistics are stored under the same key ({@link Measure.Key}), so
 * it produces only one stat event.
 * @see TemplatedCounterLink for a generalized version that keep several
 * statistics.
 */
public class CounterLink extends Link implements Measurable {

    protected final Measure.Key key;
    protected final SyncEventMeasure measure;
    protected boolean clearAfterReport;

    public CounterLink(final Measurable obo) {
        this(obo.getMeasureKey());
    }
    
    public CounterLink(final Measure.Key key) {
        super();
        this.key = key;
        this.measure = new SyncEventMeasure();
        this.clearAfterReport = false;
    }
    
    public void setClearAfterReport(final boolean enabled) {
        this.clearAfterReport = enabled;
    }

    public void process(final EventContext ectx) {
        measure.add(ectx);
        super.process(ectx);
    }
    
    public Measure.Key getMeasureKey() {
        return key;
    }
    
    public void stats(final List<Measure> measures) {
        measure.stats(key, measures, clearAfterReport);
    }
}
