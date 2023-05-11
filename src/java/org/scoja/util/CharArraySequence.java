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
 * 
 */
public class CharArraySequence implements CharSequence {

    protected final char[] data;
    protected final int off;
    protected final int len;
    
    public CharArraySequence(final char[] data) {
        this(data, 0, data.length);
    }
    
    public CharArraySequence(final char[] data, final int off, final int len) {
        this.data = data;
        this.off = off;
        this.len = len;
    }
    
    public int length() {
        return len;
    }
    
    public char charAt(final int index) {
        if (index < 0 || len <= index) outOfBounds("Index", index, 0,len-1);
        return data[off + index];
    }
    
    public CharSequence subSequence(final int start, final int end) {
        if (start < 0 || len < start) outOfBounds("Start index", start, 0,len);
        if (end < 0 || len < end) outOfBounds("Start index", end, 0,len);
        return new CharArraySequence(data, off+start, end-start);
    }
    
    public String toString() {
        return new String(data, off, len);
    }
    
    public boolean equals(final Object other) {
        return (other instanceof CharSequence)
            && equals((CharSequence)other);
    }
    
    public boolean equals(final CharSequence other) {
        if (other == null) return false;
        if (this.length() != other.length()) return false;
        final int len = this.length();
        for (int i = 0; i < len; i++) {
            if (this.charAt(i) != other.charAt(i)) return false;
        }
        return true;
    }
    
    
    protected void outOfBounds(final String what, final int idx,
                               final int min, final int max) {
        throw new IndexOutOfBoundsException(
            what + "(" + idx + ") out of bounds [" + min + "," + max + "]");
    }
}
