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

import java.util.Comparator;

import org.scoja.cc.util.Comparations;

import org.scoja.server.template.EventWriter;
import org.scoja.server.template.Template;

public class Measure {
    protected final Key key;
    protected final String concept;
    protected final long partial;
    protected final long total;
    
    public Measure(final Key key, final String concept, 
                   final long partial, final long total) {
        this.key = key;
        this.concept = concept;
        this.partial = partial;
        this.total = total;
    }
    
    public Key getKey() { return key; }
    public String getConcept() { return concept; }
    public long getPartial() { return partial; }
    public long getTotal() { return total; }
    public String getPartialAsString() { return Long.toString(partial); }
    public String getTotalAsString() { return Long.toString(total); }
    
    public String toString() {
        return "Measure[key: " + key
            + ", concept: " + concept
            + ", partial: " + partial
            + ", total: " + total
            + "]";
    }
    
    public static final Comparator<Measure> ascending
        = new Comparator<Measure>() {
            public int compare(final Measure m1, final Measure m2) {
                return Key.ascending.compare(m1.getKey(), m2.getKey());
            }
        };
        
        
    //======================================================================
    public static class Key {
        protected final String kind; //source, target, ...
        protected final String subkind;  //udp, tcp-traditional, tcp-raw, file
        protected final String parameters; //host:port, filename
        
        public Key(final String kind, final String subkind,
                   final String parameters) {
            this.kind = kind;
            this.subkind = subkind;
            this.parameters = parameters;
        }
        
        public String getKind() { return kind; }
        public String getSubkind() { return subkind; }
        public String getParameters() { return parameters; }
        
        public boolean equals(final Object other) {
            return (other instanceof Key)
                && equals((Key)other);
        }
        
        public boolean equals(final Key other) {
            return ascending.compare(this,other) == 0;
        }
        
        public int hashCode() {
            return kind.hashCode()
                + 31*subkind.hashCode()
                + 31*parameters.hashCode();
        }
        
        public String toString() {
            return "Key[" + kind + "/" + subkind + "(" + parameters + ")";
        }
        
        public static final Comparator<Key> ascending
            = new Comparator<Key>() {
                public int compare(final Key k1, final Key k2) {
                    if (k1 == null) return (k2 == null) ? 0 : -1;
                    else if (k2 == null) return 1;
                    int c = Comparations.cmp(k1.kind,k2.kind);
                    if (c != 0) return c;
                    c = Comparations.cmp(k1.subkind, k2.subkind);
                    if (c != 0) return c;
                    return Comparations.cmp(k1.parameters, k2.parameters);
                }
            };
    }
    
    //======================================================================
    public static class TemplatedKey {
        protected final EventWriter kind;
        protected final EventWriter subkind;
        protected final EventWriter parameters;
        
        public TemplatedKey(final Template kind,
                final Template subkind,
                final Template parameters) {
            this.kind = kind.asEventWriter();
            this.subkind = subkind.asEventWriter();
            this.parameters = parameters.asEventWriter();
        }
        
        public Key key(final EventContext ectx) {
            return new Key(kind.textFor(ectx), 
                    subkind.textFor(ectx), 
                    parameters.textFor(ectx));
        }
    }
}
