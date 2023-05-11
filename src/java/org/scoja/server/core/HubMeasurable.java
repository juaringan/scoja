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

import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class HubMeasurable implements Measurable {

    protected final Set<Measurable> meas;
    
    public HubMeasurable() {
        this.meas = new HashSet<Measurable>();
    }
    
    public void addMeasurable(final Measurable mea) {
        synchronized (meas) { meas.add(mea); }
    }
    
    public void removeMeasurable(final Measurable mea) {
        synchronized (meas) { meas.remove(mea); }
    }
    
    public Measure.Key getMeasureKey() {
        throw new UnsupportedOperationException("This is a Hub");
    }
    
    public void stats(final List<Measure> measures) {
        synchronized (meas) {
            for (final Measurable mea: meas) mea.stats(measures);
        }
    }
    
    public String toString() {
        return "HubMeasurable[" + meas + "]";
    }
}
