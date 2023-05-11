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

package org.scoja.server.netflow;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Map;

import org.scoja.common.DateUtils;

public abstract class Field {

    protected final String name;
    
    public Field(final String name) {
        this.name = name;
    }
    
    public Field() {
        this(null);
    }
    
    public String name() { return name; }

    public abstract long value(ByteBuffer bb);
        
    public abstract long value(Flow flow);
        
    public String repr(final Flow flow) {
        return Long.toString(value(flow));
    }

    public void addTo(final Map<String,Field> map) {
        map.put(name(), this);
    }
    
    //======================================================================
    public static class RelativeFlowSeq extends Field {
        public RelativeFlowSeq(final String name) { super(name); }
        
        public long value(final ByteBuffer bb) {
            throw new UnsupportedOperationException();
        }
        
        public long value(final Flow flow) { return flow.relativeFlowSeq(); }
    }
    
    //======================================================================
    public static abstract class Extracting extends Field {
        protected final boolean header;
        protected final int pos;
        
        public Extracting(final String name, 
                final boolean header, final int pos) {
            super(name);
            this.header = header;
            this.pos = pos;
        }
        
        public Extracting(final boolean header, final int pos) {
            this(null, header, pos);
        }
    }
    
    public static class UInt extends Extracting {
        public UInt(final String name, final boolean header, final int pos) {
            super(name, header, pos);
        }
        
        public UInt(final boolean header, final int pos) {
            super(header, pos);
        }
        
        public long value(final ByteBuffer bb) {
            return BBTrait.getUInt(bb, pos);
        }
        
        public long value(final Flow flow) {
            return flow.getUInt(header, pos);
        }
    }
    
    public static class UShort extends Extracting {
        public UShort(final String name, final boolean header, final int pos) {
            super(name, header, pos);
        }
        
        public UShort(final boolean header, final int pos) {
            super(header, pos);
        }
        
        public long value(final ByteBuffer bb) {
            return BBTrait.getUShort(bb, pos);
        }
        
        public long value(final Flow flow) {
            return flow.getUShort(header, pos);
        }
    }
    
    public static class UByte extends Extracting {
        public UByte(final String name, final boolean header, final int pos) {
            super(name, header, pos);
        }
        
        public UByte(final boolean header, final int pos) {
            super(header, pos);
        }
        
        public long value(final ByteBuffer bb) {
            return BBTrait.getUByte(bb, pos);
        }
        
        public long value(final Flow flow) {
            return flow.getUByte(header, pos);
        }
    }
    
    public static class SecNanos extends Extracting {
        public SecNanos(final String name, 
                final boolean header, final int pos) {
            super(name, header, pos);
        }
        
        public SecNanos(final boolean header, final int pos) {
            super(header, pos);
        }
        
        public long value(final ByteBuffer bb) {
            return BBTrait.getUInt(bb,pos)*1000
                + BBTrait.getUInt(bb,pos+4)/1000000;
        }
        
        public long value(final Flow flow) {
            return flow.getUInt(header,pos)*1000
                + flow.getUInt(header,pos+4)/1000000;
        }
    }
    
    
    //======================================================================
    public static class Const extends Field {
        protected final long value;
        public Const(final long value) { this.value = value; }
        public long value(final ByteBuffer bb) { return value; }
        public long value(final Flow flow) { return value; }
    }
    
    public static abstract class Binary extends Field {
        protected final Field expr1;
        protected final Field expr2;
        
        public Binary(final String name, final Field expr1, final Field expr2) {
            super(name);
            if (expr1 == null || expr2 == null)
                throw new IllegalArgumentException();
            this.expr1 = expr1;
            this.expr2 = expr2; 
        }
        
        public Binary(final Field expr1, final Field expr2) {
            this(null, expr1, expr2);
        }
        
        protected abstract long op(long v1, long v2);
        
        public long value(final ByteBuffer bb) {
            return op(expr1.value(bb), expr2.value(bb));
        }
        
        public long value(final Flow flow) {
            return op(expr1.value(flow), expr2.value(flow));
        }
    }
    
    public static class Add extends Binary {
        public Add(final String name, final Field expr1, final Field expr2) {
            super(name, expr1, expr2);
        }
        
        public Add(final Field expr1, final Field expr2) {
            super(expr1, expr2);
        }
        
        protected long op(final long v1, final long v2) { return v1 + v2; }
    }
    
    public static class Sub extends Binary {
        public Sub(final String name, final Field expr1, final Field expr2) {
            super(name, expr1, expr2);
        }
        
        public Sub(final Field expr1, final Field expr2) {
            super(expr1, expr2);
        }
        
        protected long op(final long v1, final long v2) { return v1 - v2; }
    }
    
    public static class Div extends Binary {
        public Div(final String name, final Field expr1, final Field expr2) {
            super(name, expr1, expr2);
        }
        
        public Div(final Field expr1, final Field expr2) {
            super(expr1, expr2);
        }
        
        protected long op(final long v1, final long v2) {
            return (v2 == 0) ? Long.MAX_VALUE : (v1 / v2);
        }
    }
    
    public static class Mul extends Binary {
        public Mul(final String name, final Field expr1, final Field expr2) {
            super(name, expr1, expr2);
        }
        
        public Mul(final Field expr1, final Field expr2) {
            super(expr1, expr2);
        }
        
        protected long op(final long v1, final long v2) { return v1 * v2; }
    }
    
    //======================================================================
    public static abstract class Format extends Field {
        protected final Field base;
        
        public Format(final String name, final Field base) {
            super(name);
            if (base == null) throw new IllegalArgumentException();
            this.base = base;
        }
        
        public Format(final Field base) {
            this(null, base);
        }
        
        public long value(final ByteBuffer bb) {
            return base.value(bb);
        }
        
        public long value(final Flow flow) {
            return base.value(flow);
        }
    }
    
    public static class IP extends Format {
        public IP(final String name, final Field base) { super(name, base); }
        public IP(final Field base) { super(base); }
        public String repr(final Flow flow) {
            final int value = (int)value(flow);
            return (value >>> 24) 
                + "." + ((value >>> 16) & 0xFF)
                + "." + ((value >>> 8) & 0xFF)
                + "." + (value & 0xFF);
        }
    }
    
    public static class Date extends Format {
        public Date(final String name, final Field base) { super(name, base); }
        public Date(final Field base) { super(base); }
        public String repr(final Flow flow) {
            final Calendar date = Calendar.getInstance();
            date.setTimeInMillis(value(flow));
            return DateUtils.formatGMT(date);
        }
    }
}