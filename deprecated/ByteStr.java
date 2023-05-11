
package org.scoja.server.core;

import java.io.OutputStream;
import java.io.IOException;


public class ByteStr implements CharSequence {
    
    protected final byte[] data;
    protected final int start;
    protected final int length;
    
    public ByteStr(final byte[] data) {
        this(data, 0);
    }
    
    public ByteStr(final byte[] data, final int start) {
        this(data, start, data.length-start);
    }
    
    public ByteStr(final byte[] data, final int start, final int length) {
        this.data = data;
        this.start = start;
        this.length = length;
    }
    
    public char charAt(final int index) {
        if (index < 0 || length <= index) {
            throw new IndexOutOfBoundsException
                ("Index " + index + " out of [0," + length +")");
        }
        return (char)(data[start+index] & 0xff);
    }
    
    public int length() {
        return length;
    }
    
    public CharSequence subSequence(final int start, final int end) {
        return new ByteStr(data, this.start+start, end-start);
    }
    
    public void writeTo(final OutputStream out) throws IOException {
        out.write(data, start, length);
    }
    
    //======================================================================
    public String toString() {
        final char[] cs = new char[length()];
        for (int i = 0; i < cs.length; i++) {
            cs[i] = (char)(data[start+i] & 0xff);
        }
        return new String(cs);
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
