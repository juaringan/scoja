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

/**
 * Similar {@link java.lang.StringBuffer} but with special methods to format
 * sequences.
 * By default, two last elements are separated with "<tt> and </tt>"
 * and previous onew are separted with commas ("<tt>, </tt>").
 */
public class SeqBuffer {

    protected final StringBuffer main;
    
    protected StringBuffer item;
    protected final String isep;
    protected final String lsep;
    protected int flushedItems;
    
    protected StringBuffer active;
    
    public SeqBuffer() {
        this(new StringBuffer());
    }
    
    public SeqBuffer(final String isep, final String lsep) {
        this(new StringBuffer(), isep, lsep);
    }
    
    public SeqBuffer(final int initSize) {
        this(new StringBuffer(initSize));
    }
    
    public SeqBuffer(final int initSize,
                     final String isep, final String lsep) {
        this(new StringBuffer(initSize), isep, lsep);
    }
    
    public SeqBuffer(final StringBuffer main) {
        this(main, ", ", " and ");
    }
    
    public SeqBuffer(final StringBuffer main,
                     final String isep, final String lsep) {
        this.main = this.active = main;
        this.item = null;
        this.isep = isep;
        this.lsep = lsep;
    }

    
    //======================================================================
    public SeqBuffer init() {
        end();
        if (item == null) item = new StringBuffer();
        active = item;
        flushedItems = 0;
        return this;
    }
    
    public SeqBuffer item() {
        if (active != item) init();
        flushCurrentItem(isep);
        return this;
    }
    
    public SeqBuffer end() {
        if (active != item) return this;
        flushCurrentItem(lsep);
        active = main;
        return this;
    }
    
    protected void flushCurrentItem(final String sep) {
        if (item.length() == 0) return;
        if (flushedItems > 0) main.append(sep);
        main.append(item);
        item.delete(0, item.length());
        flushedItems++;
    }
    
    public StringBuffer current() {
        return active;
    }
    
    //======================================================================
    public SeqBuffer clear() {
        main.delete(0, main.length());
        item.delete(0, item.length());
        active = main;
        return this;
    }
    
    
    //======================================================================
    public SeqBuffer append(final boolean b) {
        active.append(b);
        return this;
    }
    
    public SeqBuffer append(final char c) {
        active.append(c);
        return this;
    }
    
    public SeqBuffer append(final char[] str) {
        active.append(str);
        return this;
    }
    
    public SeqBuffer append(final char[] str, final int off, final int len) {
        active.append(str, off, len);
        return this;
    }
    
    public SeqBuffer append(final double d) {
        active.append(d);
        return this;
    }
    
    public SeqBuffer append(final float f) {
        active.append(f);
        return this;
    }
    
    public SeqBuffer append(final int i) {
        active.append(i);
        return this;
    }
    
    public SeqBuffer append(final long l) {
        active.append(l);
        return this;
    }
    
    public SeqBuffer append(final Object obj) {
        active.append(obj);
        return this;
    }
    
    public SeqBuffer append(final String str) {
        active.append(str);
        return this;
    }
    
    public SeqBuffer append(final StringBuffer sb) {
        active.append(sb);
        return this;
    }
    
    public SeqBuffer append(final SeqBuffer sb) {
        sb.appendTo(this);
        return this;
    }
    
    public void appendTo(final SeqBuffer sb) {
        sb.append(main);
        if (active != main) sb.append(active);
    }
    
    
    //======================================================================
    public String toString() {
        end();
        return main.toString();
    }
}
