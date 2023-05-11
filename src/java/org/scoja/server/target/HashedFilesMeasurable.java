/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2008  Bankinter, SA.
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

package org.scoja.server.target;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scoja.server.core.CPUUsage;
import org.scoja.server.core.Measure;
import org.scoja.util.TransientMap;

public class HashedFilesMeasurable implements FilesMeasurable {

    protected final Object partialLock;
    protected Map<String,FileStats> partial;
    protected final TransientMap total;

    public HashedFilesMeasurable(final TransientMap total) {
        this.partialLock = new Object();
        this.partial = new HashMap<String,FileStats>();
        this.total = total;
    }
    
    public void written(final String filename, final boolean opened,
                        final int events, final int bytes,
                        final CPUUsage cpu) {
        FileStats fs;
        synchronized (partialLock) {
            fs = partial.get(filename);
            if (fs == null) {
                fs = new FileStats();
                partial.put(filename, fs);
            }
        }
        synchronized (fs) { fs.written(opened, events, bytes, cpu); }
    }
    
    public Measure.Key getMeasureKey() {
        throw new UnsupportedOperationException();
    }
    
    public void stats(final List<Measure> measures) {
        final Map<String,FileStats> pp;
        synchronized (partialLock) {
            pp = partial;
            partial = new HashMap<String,FileStats>();
        }
        for (final Map.Entry<String,FileStats> e: pp.entrySet()) {
            final String file = e.getKey();
            final FileStats ps = e.getValue();
            final long to, po = ps.getOpens();
            final long te, pe = ps.getEvents();
            final long tb, pb = ps.getBytes();
            final CPUUsage tc, pc = ps.getCPU();
            final FileStats ts = (FileStats)total.get(file);
            if (ts == null) {
                total.put(file, ps);
                to = po;  te = pe;  tb = pb;  tc = pc;
            } else {
                ts.add(ps);
                to = ts.getOpens(); te = ts.getEvents(); tb = ts.getBytes();
                tc = ts.getCPU();
            }
            final Measure.Key key = new Measure.Key("target","file",file);
            measures.add(new Measure(key, "opens", po, to));
            measures.add(new Measure(key, "events", pe, te));
            measures.add(new Measure(key, "bytes", pb, tb));
            measures.add(new Measure(key, "real-time",
                                     tc.getRealTime(), pc.getRealTime()));
            measures.add(new Measure(key, "cpu",
                                     tc.getCPUTime(), pc.getCPUTime()));
            measures.add(new Measure(key, "cpu-user",
                                     tc.getUserTime(), pc.getUserTime()));
        }
    }
    
    
    //======================================================================
    private static class FileStats {
        protected long opens;
        protected long events;
        protected long bytes;
        protected CPUUsage cpu;
        
        public FileStats() {
            this.opens = 0;
            this.events = 0;
            this.bytes = 0;
            this.cpu = CPUUsage.none();
        }
        
        public void written(final boolean opened, final int es, final int bs,
                            final CPUUsage cpu) {
            if (opened) this.opens++;
            this.events += es;
            this.bytes += bs;
            if (cpu != null) this.cpu.inc(cpu);
        }
     
        public void add(final FileStats other) {
            this.opens += other.opens;
            this.events += other.events;
            this.bytes += other.bytes;
            this.cpu.inc(other.cpu);
        }
        
        public long getOpens() { return opens; }
        public long getEvents() { return events; }
        public long getBytes() { return bytes; }
        public CPUUsage getCPU() { return cpu; }
    }
}
