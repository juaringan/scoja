/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2010  Mario Martínez
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
package org.scoja.trans;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.ArrayList;
import org.scoja.cc.lang.Procedure1;
import org.scoja.cc.io.Charsets;

public interface IncrementalParser<A> {

    public void reset();

    public boolean isDone();
    
    public A getParsedValue();

    public int parse(byte[] data, int off, int len)
    throws SyntaxException;
    

    //======================================================================
    public static class Literal implements IncrementalParser<byte[]> {    
        protected final byte[] expected;
        protected int next;
        
        public Literal(final byte[] expected) {
            this.expected = expected;
            this.next = 0;
        }
    
        public Literal(final String expected) {
            this(Charsets.latin1(expected));
        }
        
        public void reset() {
            this.next = 0;
        }
        
        public boolean isDone() {
            return next == expected.length;
        }
        
        public byte[] getParsedValue() {
            return expected;
        }
        
        public int parse(final byte[] data, final int off, final int len)
        throws SyntaxException {
            final int n = Math.min(len, expected.length-next);
            for (int i = 0; i < n; i++) {
                if (expected[next+i] != data[off+i]) throw new SyntaxException(
                    "Expected `" + Charsets.latin1(expected)
                    + "' received `" + Charsets.latin1(expected,0,next+i)
                    + (char)data[off+i] + "'");
            }
            next += n;
            return n;
        }
    }

    
    //======================================================================
    public static class Space implements IncrementalParser<Integer> {
        
        protected int count;
        protected boolean done;
        
        public Space() {
            this.count = 0;
            this.done = false;
        }
        
        public void reset() {
            count = 0;
            done = false;
        }
        
        public boolean isDone() {
            return done;
        }
        
        public Integer getParsedValue() {
            return count;
        }
        
        public int parse(final byte[] data, final int off, final int len)
        throws SyntaxException {
            int i = 0;
            while (!done && i < len) {
                if (data[off+i] == ' ') i++;
                else done = true;
            }
            return i;
        }
    }
    
    
    //======================================================================
    public static class Upto implements IncrementalParser<byte[]> {
        protected final byte[] del;
        protected final ByteArrayOutputStream store;
        protected int matched;
        
        public Upto(final byte[] del, final boolean capture) {
            this.del = del;
            this.store = capture ? new ByteArrayOutputStream() : null;
            this.matched = 0;
        }
        
        public Upto(final String del, final boolean capture) {
            this(Charsets.latin1(del), capture);
        }
        
        public void reset() {
            if (store != null) store.reset();
            matched = 0;
        }
        
        public boolean isDone() {
            return matched == del.length;
        }
        
        public byte[] getParsedValue() {
            return (store == null) ? null : store.toByteArray();
        }
        
        public int parse(final byte[] data, final int off, final int len)
        throws SyntaxException {
            int i = 0;
            while (matched < del.length && i < len) {
                if (data[off+i] == del[matched]) { i++; matched++; }
                else if (matched > 0) matched = 0;
                else i++;
            }
            if (store != null) store.write(data, off, i);
            return i;
        }
    }
    
    
    //======================================================================
    public static class Nat implements IncrementalParser<Long> {
        protected long value;
        protected int digits;
        protected boolean done;
        
        public Nat() {
            this.value = 0;
            this.digits = 0;
            this.done = false;
        }
        
        public void reset() {
            value = 0;
            digits = 0;
            done = false;
        }
        
        public boolean isDone() {
            return done;
        }
        
        public Long getParsedValue() {
            return value;
        }
        
        public int parse(final byte[] data, final int off, final int len)
        throws SyntaxException {
            int i = 0;
            while (i < len) {
                final int d = value(data[off+i]);
                if (d != -1) {
                    value = 10*value + d;
                    digits++;
                    i++;
                } else if (digits == 0) {
                    throw new SyntaxException(
                        "Empty natural (next char " + (char)data[off+i] + ")");
                } else {
                    done = true;
                    break;
                }
            }
            return i;
        }
        
        protected int value(final byte digit) {
            return ('0' <= digit && digit <= '9') ? (digit - '0') : -1;
        }
    }
    
    
    //======================================================================
    public static class Seq<A> implements IncrementalParser<List<A>> {
    
        protected final IncrementalParser<? extends A>[] parsers;
        protected int next;
        
        public Seq(final IncrementalParser<? extends A>[] parsers) {
            this.parsers = parsers;
            this.next = 0;
        }
        
        public void reset() {
            for (int i = 0; i < next; i++) parsers[i].reset();
            next = 0;
        }
        
        public boolean isDone() {
            return next >= parsers.length;
        }
    
        public List<A> getParsedValue() {
            final List<A> vs = new ArrayList<A>(parsers.length);
            for (final IncrementalParser<? extends A> p: parsers) 
                vs.add(p.getParsedValue());
            return vs;
        }

        public int parse(final byte[] data, final int off, final int len)
        throws SyntaxException {
            int i = 0;
            while (next < parsers.length && i < len) {
                i += parsers[next].parse(data, off+i, len-i);
                //System.err.println("Ends at " + i);
                if (!parsers[next].isDone()) break;
                next++;
            }
            return i;
        }
    }
    
    
    //======================================================================
    public static class Until<A> implements IncrementalParser<A> {
        protected final IncrementalParser<A> base;
        protected final byte[] expected;
        protected int next;

        public Until(final byte[] expected, final IncrementalParser<A> base) {
            this.base = base;
            this.expected = expected;
            this.next = 0;
        }
        
        public Until(final String expected, final IncrementalParser<A> base) {
            this(Charsets.latin1(expected), base);
        }
        
        public void reset() {
            base.reset();
            next = 0;
        }

        public boolean isDone() {
            return next == expected.length;
        }
    
        public A getParsedValue() {
            return base.getParsedValue();
        }

        public int parse(final byte[] data, final int off, final int len)
        throws SyntaxException {
            int total = 0;
            while (total < len) {
                if (next < 0) {
                    final int p = base.parse(data, off+total, len-total);
                    total += p;
                    if (p == 0)
                        if (base.isDone()) { base.reset(); next = 0; }
                        else break;
                } else {
                    final int n = Math.min(len-total, expected.length-next);
                    int i = 0;
                    while (i < n && expected[next+i] == data[off+total+i]) i++;
                    if (i == n) {
                        next += i;
                        total += i;
                        if (next == expected.length) break;
                    } else {
                        final int rewind = Math.min(next, total);
                        next -= rewind;
                        total -= rewind;
                        if (next > 0) {
                            final int p = base.parse(expected, 0, next);
                            if (p != next) throw new SyntaxException(
                                "Illegal parser: a limited parsed didn't"
                                + " consume the separator; given " + next
                                + " consumed " + p);
                        }
                        next = -1;
                    }
                }
            }
            return total;
        }
    }
        
        
    //======================================================================
    public static class WhenDone<A> implements IncrementalParser<A> {
    
        protected final Procedure1<IncrementalParser<A>> action;
        protected final IncrementalParser<A> base;
        protected boolean done;
        
        public WhenDone(final Procedure1<IncrementalParser<A>> action,
                final IncrementalParser<A> base) {
            this.action = action;
            this.base = base;
            this.done = false;
        }
        
        public void reset() {
            base.reset();
            done = false;
        }

        public boolean isDone() {
            return done;
        }
    
        public A getParsedValue() {
            return base.getParsedValue();
        }

        public int parse(final byte[] data, final int off, final int len)
        throws SyntaxException {
            if (done) return 0;
            final int n = base.parse(data, off, len);
            if (base.isDone()) {
                done = true;
                action.exec(base);
            }
            return n;
        }
    }
}
