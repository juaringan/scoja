
package org.scoja.server.core;

import java.io.PrintWriter;


public class CharStr implements CharSequence {
    
    protected final char[] data;
    protected final int start;
    protected final int length;
    
    public CharStr(final char[] data) {
        this(data, 0);
    }
    
    public CharStr(final char[] data, final int start) {
        this(data, start, data.length-start);
    }
    
    public CharStr(final char[] data, final int start, final int length) {
        this.data = data;
        this.start = start;
        this.length = length;
    }
    
    public char charAt(final int index) {
        if (index < 0 || length <= index) {
            throw new IndexOutOfBoundsException
                ("Index " + index + " out of [0," + length +")");
        }
        return data[start+index];
    }
    
    public int length() {
        return length;
    }
    
    public CharSequence subSequence(final int start, final int end) {
        return new CharStr(data, this.start+start, end-start);
    }
    
    public void writeTo(final PrintWriter out) {
        out.write(data, start, length);
    }
    
    //======================================================================
    public String toString() {
        return new String(data, start, length());
    }
    
    public boolean equals(final Object other) {
        return (other instanceof CharSequence) 
            && equals((CharSequence)other);
    }
    
    public boolean equals(final CharSequence other) {
        if (this.length != other.length()) return false;
        for (int i = 0; i < length; i++) {
            if ((data[start+i] & 0xff) != other.charAt(i)) return false;
        }
        return true;
    }
}
