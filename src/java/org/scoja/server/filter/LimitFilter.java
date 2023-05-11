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

package org.scoja.server.filter;

import java.util.Map;
import java.util.HashMap;

import org.scoja.cc.lang.Pair;
import org.scoja.cc.lang.Procedure1;
import org.scoja.cc.lang.PendingWork;
import org.scoja.cc.scache.LRUMap;
import org.scoja.cc.scache.EvictionListener;

import org.scoja.server.core.EventContext;
import org.scoja.server.expr.StringExpression;
import org.scoja.server.source.Internal;

public class LimitFilter 
    extends FilterLinkableAtPython 
    implements EvictionListener<String,Map<String,LimitFilter.Stat>> {

    public static final long UNLIMITED = -1;
    
    protected final StringExpression key;
    protected final StringExpression age;
    
    protected final LRUMap<String,Map<String,Stat>> age2key2stat;
    protected final PendingWork<Pair<String,Map<String,Stat>>> evictedKeys;
    
    protected String description;
    protected long maxSize;
    protected long maxEvents;

    public LimitFilter(final StringExpression key, 
            final StringExpression age, final long forgetAfter) {
        this.age = age;
        this.key = key;
        this.age2key2stat = new LRUMap<String,Map<String,Stat>>();
        this.age2key2stat.setMaxLife(forgetAfter);
        this.age2key2stat.setEvictionListener(this);
        this.evictedKeys = new PendingWork<Pair<String,Map<String,Stat>>>(
            new Procedure1<Pair<String,Map<String,Stat>>>() {
                public void exec(final Pair<String,Map<String,Stat>> pair) {
                    showEvicted(pair.getFirst(), pair.getSecond());
                }
            });
        this.description = null;
        this.maxSize = this.maxEvents = UNLIMITED;
    }
    
    public void setMaxEvents(final long maxEvents) {
        this.maxEvents = maxEvents;
    }
    
    public void setMaxSize(final long maxSize) {
        this.maxSize = maxSize;
    }
    
    public void setDescription(final String description) {
        this.description = description;
    }
    
    public boolean isGood(final EventContext ectx) {
        final int bytes = ectx.getEvent().getByteSize();
        final String agestr
            = (age == null) ? null : age.eval(ectx).unqualified();
        Map<String,Stat> key2stat;
        synchronized (age2key2stat) {
            key2stat = age2key2stat.get(agestr);
            if (key2stat == null) {
                key2stat = new HashMap<String,Stat>();
                age2key2stat.put(agestr, key2stat);
            }
        }
        evictedKeys.perform();
        final String keystr = key.eval(ectx).unqualified();
        Stat stat;
        synchronized (key2stat) {
            stat = key2stat.get(keystr);
            if (stat == null) {
                stat = new Stat();
                key2stat.put(keystr, stat);
            }
        }
        final boolean result, limitTouched;
        synchronized (stat) {
            if (stat.isBlocked()) { 
                result = limitTouched = false; 
            } else if (!stat.reachedLimit()) {
                result = true; limitTouched = false;
            } else {
                stat.block();
                result = false; limitTouched = true;
            }
            stat.considerEvent(bytes);
        }
        if (limitTouched) {
            Internal.warning(ectx, Internal.FILTER_LIMIT, 
                    "Reached limit (events " + maxEvents + ", size " + maxSize
                    + ") for " + description() + " at age `" + agestr
                    + "' and key `" + keystr + "'");
        }
        return result;
    }
    
    public void evicted(final String key,
            final Map<String,Stat> value) {
        evictedKeys.add(new Pair<String,Map<String,Stat>>(key, value));
    }
    
    protected void showEvicted(final String age, 
            final Map<String,Stat> map) {
        Internal.info(Internal.FILTER_LIMIT, 
                "Limit info for " + description() + " at age `" + age
                + "' has been dropped");
        for (final Map.Entry<String,Stat> entry: map.entrySet()) {
            final Stat stat = entry.getValue();
            if (!stat.isBlocked()) continue;
            Internal.info(Internal.FILTER_LIMIT,
                    "Limit info for " + description() + " at age `" + age
                    + "' on `" + entry.getKey() + "' dropped elements"
                    + "; total events: " + stat.events
                    + ", total size: " + stat.size
                    + ", passed events: " + stat.passedEvents
                    + ", passed size: " + stat.passedSize);
        }
    }

    protected String description() {
        return (description == null) ? "?" : description;
    }
        
    public String toString() {
        return "testing whether events on " + description() + " at `" + age
            + "'/`" + key + "' don't exceed count " + maxEvents
            + ", size " + maxSize;
    }
    
    
    //======================================================================
    public class Stat {

        protected long events;
        protected long size;
        protected boolean blocked;
        protected long passedEvents;
        protected long passedSize;
        
        public Stat() {
            this.events = 0;
            this.size = 0;
            this.blocked = false;
        }
        
        public boolean isBlocked() { return blocked; }
        
        public void block() { 
            if (!blocked) {
                blocked = true;
                passedEvents = events;
                passedSize = size;
            }
        }
        
        public void considerEvent(final int size) {
            this.events++;
            this.size += size;
        }
        
        public boolean reachedLimit() {
            return (maxEvents > 0 && events >= maxEvents)
                || (maxSize > 0 && size >= maxSize);
        }
        
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Stat[events: ").append(events)
                .append(", size: ").append(size);
            if (blocked) {
                sb.append(", passed events: ").append(passedEvents)
                    .append(", passed size: ").append(passedSize);
            }
            return sb.append("]").toString();
        }
    }
}
