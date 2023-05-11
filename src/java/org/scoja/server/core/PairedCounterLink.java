/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2012  LogTrust
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

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.scoja.cc.lang.Pair;
import org.scoja.server.template.Template;

public class PairedCounterLink extends Link implements Measurable {

    protected final Template kind;
    protected final Template subkind;
    protected final Template first;
    protected final Template parameters;
    protected final Template last;
    protected final Pattern selector;
    protected final String pairSeparator;
    
    protected int max;
    protected String excessKind;
    protected String excessSubkind;
    protected String excessPair;
    protected boolean clearAfterReport;
    
    protected final Object lock;
    protected Map<Object,Group> map;

    public PairedCounterLink(final Template kind, final Template subkind,
            final Template first, final Template parameters, 
            final Template last,
            final String selector, final String pairSeparator) {
        this.kind = kind;
        this.subkind = subkind;
        this.first = first;
        this.parameters = parameters;
        this.last = last;
        this.selector = Pattern.compile(selector);
        this.pairSeparator = pairSeparator;
        
        this.max = -1;
        this.excessKind = this.excessSubkind = this.excessPair = null;
        this.clearAfterReport = false;
        
        this.lock = new Object();
        this.map = new HashMap<Object,Group>();
    }
    
    public void setClearAfterReport(final boolean enable) {
        this.clearAfterReport = enable;
    }
    
    public void setBound(final int max,
            final String excessKind,
            final String excessSubkind,
            final String excessPair) {
        if (max > 0 && excessPair == null) throw new IllegalArgumentException(
            "Identifier for the excess case is compulsory when bounding");
        this.max = max;
        this.excessKind = excessKind;
        this.excessSubkind = excessSubkind;
        this.excessPair = excessPair;
    }
    
    public Measure.Key getMeasureKey() {
        throw new UnsupportedOperationException("This is a templated counter");
    }
    
    public void process(final EventContext ectx) {
        final List<String> pairs = allPairs(ectx);
        if (pairs.isEmpty()) return;
        final String kindStr = kind.textFor(ectx);
        final String subkindStr = subkind.textFor(ectx);
        final Object grpkey = groupKey(kindStr, subkindStr);
        synchronized (lock) {
            final Group group = ensureGroup(grpkey);
            for (final String pair: pairs) {
                group.process(new Measure.Key(kindStr,subkindStr,pair), ectx); 
            }
        }
        super.process(ectx);
    }    
    
    protected List<String> allPairs(final EventContext ectx) {
        final String firstStr = textFor(first, ectx);
        final String sourceStr = parameters.textFor(ectx);
        final String lastStr = textFor(last, ectx);
        final List<String> subkinds = new ArrayList<String>();
        String prev = firstStr;
        final Matcher matcher = selector.matcher(sourceStr);
        while (matcher.find()) {
            final String curr = concatGroups(matcher);
            if (prev != null) subkinds.add(prev + pairSeparator + curr);
            prev = curr;
        }
        if (prev != null && lastStr != null)
            subkinds.add(prev + pairSeparator + lastStr);
        return subkinds;
    }
    
    protected String textFor(final Template temp, final EventContext ectx) {
        return (temp == null) ? null : temp.textFor(ectx);
    }
    
    protected String concatGroups(final Matcher matcher) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 1, l = matcher.groupCount(); i <= l; i++) 
            sb.append(matcher.group(i));
        return sb.toString();
    }

    protected Object groupKey(final String kind, final String subkind) {
        return (excessKind == null) 
            ? ((excessSubkind == null) ? Pair.make(kind,subkind) : kind)
            : ((excessSubkind == null) ? subkind : null);
    }
    
    protected Measure.Key excessKey(final Object grpKey) {
        final String kindStr, subkindStr;
        if (excessKind == null) {
            if (excessSubkind == null) {
                @SuppressWarnings("unchecked")
                final Pair<String,String> pair = (Pair<String,String>)grpKey;
                kindStr = pair.getFirst();
                subkindStr = pair.getSecond();
            } else {
                kindStr = (String)grpKey;
                subkindStr = excessSubkind;
            }
        } else {
            kindStr = excessKind;
            if (excessSubkind == null) subkindStr = (String)grpKey;
            else subkindStr = excessSubkind;
        }
        return new Measure.Key(kindStr, subkindStr, excessPair);
    }
        
    protected Group ensureGroup(final Object grpKey) {
        Group group = map.get(grpKey);
        if (group == null) {
            group = new Group(grpKey);
            map.put(grpKey, group);
        }
        return group;
    }
    
    public void stats(final List<Measure> measures) {
        final Map<Object,Group> oldmap;
        synchronized (lock) {
            oldmap = map;
            map = new HashMap<Object,Group>();
        }
        for (final Group group: oldmap.values()) group.stats(measures);
        if (clearAfterReport) return;
        synchronized (lock) {
            for (final Map.Entry<Object,Group> e1: oldmap.entrySet()) {
                final Object grpkey = e1.getKey();
                final Group oldgrp = e1.getValue();
                final Group newgrp = map.get(grpkey);
                if (newgrp == null) map.put(grpkey, oldgrp);
                else newgrp.add(oldgrp);
            }
        }
    }    
    
    
    //======================================================================
    public class Group {
        protected final Object key;
        protected final Map<Measure.Key,EventMeasure> map;
        protected EventMeasure excess;
        
        public Group(final Object key) {
            this.key = key;
            this.map = new HashMap<Measure.Key,EventMeasure>();
            this.excess = null;
        }
        
        public void process(final Measure.Key key, final EventContext ectxt) {
            EventMeasure measure = map.get(key);
            if (measure == null) {
                if (max <= 0 || map.size() < max) {
                    measure = new EventMeasure();
                    map.put(key, measure);
                } else {
                    if (excess == null) excess = new EventMeasure();
                    measure = excess;
                }
            }
            measure.add(ectxt);
        }
        
        public void stats(final List<Measure> measures) {
            if (excess != null) excess.stats(excessKey(key), measures);
            for (final Map.Entry<Measure.Key,EventMeasure> entry
                     : map.entrySet()) {
                entry.getValue().stats(entry.getKey(), measures);
            }
        }
        
        public void add(final Group old) {
            if (old.excess != null) addToExcess(old.excess);
            final Iterator<Map.Entry<Measure.Key,EventMeasure>> oit
                = old.map.entrySet().iterator();
            while (oit.hasNext()) {
                final Map.Entry<Measure.Key,EventMeasure> entry = oit.next();
                final Measure.Key key = entry.getKey();
                final EventMeasure oldmea = entry.getValue();
                final EventMeasure newmea = map.get(key);
                if (newmea != null) newmea.add(oldmea);
                else if (max <= 0 || map.size() < max) map.put(key,oldmea);
                else { addToExcess(oldmea); break; }
            }
            while (oit.hasNext()) {
                this.excess.add(oit.next().getValue());
            }
        }
        
        protected void addToExcess(final EventMeasure mea) {
            if (this.excess == null) this.excess = mea;
            else this.excess.add(mea);
        }
    }
}
