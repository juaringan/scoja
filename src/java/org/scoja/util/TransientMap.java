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

package org.scoja.util;

import java.util.Map;
import java.util.HashMap;

/**
 * Es una tabla que recuerda información sólo durante un cierto
 * tiempo que llamaremos <i>fading-out time</i>.
 * Cuando se intenta acceder a un elemento que lleva más del
 * <i>fading-out time</i>, esta tabla dice que no lo conoce como si
 * nunca se hubiera añadido.
 * <p>
 * Además es posible añadir un límite de entradas totales.
 * Si se añade una entrada que excede este límite y no hay ningún
 * elemento que olvidar, se olvidarán a los más viejos.
 */
public class TransientMap {

    public static final int NO_SIZE_LIMIT = -1;

    protected int maxSize;
    protected long fadingOut;
    protected Graveyard graveyard;
    protected final Map map;
    protected final Link queue;
    
    public TransientMap(final long fadingOut) {
        this(NO_SIZE_LIMIT, fadingOut);
    }
    
    public TransientMap(final int maxSize, final long fadingOut) {
        this.maxSize = maxSize;
        this.fadingOut = fadingOut;
        this.graveyard = null;
        this.map = new HashMap();
        this.queue = new Link();
    }
    
    public void setSize(final int maxSize) {
        this.maxSize = maxSize;
    }
    
    public void setFadingOut(final long fadingOut) {
        this.fadingOut = fadingOut;
    }
    
    public void setGraveyard(final Graveyard graveyard) {
        this.graveyard = graveyard;
    }
    
    public synchronized void put(final Object key, final Object value) {
        Link link = (Link)map.get(key);
        if (link != null) {
            link.setValue(value);
            link.unlink();
            link.linkAfter(queue);
            killForgotten(0);
        } else {
            killForgotten(1);
            link = new Link(key, value);
            link.linkAfter(queue);
            map.put(key, link);
        }
    }
    
    public synchronized Object get(final Object key) {
        killForgotten(0);
        final Link link = (Link)map.get(key);
        return (link != null) ? link.getValue() : null;
    }
    
    protected void killForgotten(final int toAdd) {
        final long killtime = System.currentTimeMillis() - fadingOut;
        int toKill;
        if (maxSize == NO_SIZE_LIMIT) toKill = 0;
        else toKill = map.size() + toAdd - maxSize;
        Link current = queue.getPrevious();
        while (current != queue
               && (toKill > 0
                   || current.getDefinitionTime() <= killtime)) {
            final Link next = current.getPrevious();
            kill(current);
            toKill--;
            current = next;
        }
    }
    
    protected void kill(final Link link) {
        final Object key = link.getKey();
        link.unlink();
        map.remove(key);
        if (graveyard != null) {
            graveyard.died(key, link.getValue());
        }
    }
    
    
    //======================================================================
    private static class Link {
        protected final Object key;
        protected Object value;
        protected long definitionTime;
        
        protected Link prev;
        protected Link next;
        
        public Link() {
            this(null, null);
        }
        
        public Link(final Object key, final Object value) {
            this.key = key;
            this.prev = this.next = this;
            setValue(value);
        }
        
        public Object getKey() {
            return key;
        }
        
        public Object getValue() {
            return value;
        }
        
        public long getDefinitionTime() {
            return definitionTime;
        }
        
        public void setValue(final Object value) {
            this.value = value;
            this.definitionTime = System.currentTimeMillis();
        }
        
        public Link getPrevious() {
            return prev;
        }
        
        protected void unlink() {
            next.prev = prev;
            prev.next = next;
        }
        
        protected void linkAfter(final Link link) {
            prev = link;
            next = link.next;
            this.next.prev = this;
            this.prev.next = this;
        }
    }
}
