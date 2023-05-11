/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser/Library General Public License
 * as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
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

package org.scoja.io;

import java.io.InputStream;
import java.io.IOException;

public class EmptyInputStream extends InputStream {

    private static final EmptyInputStream instance = new EmptyInputStream();
    
    public static EmptyInputStream getInstance() {
        return instance;
    }
    
    public int read() { return -1; }
    public int read(final byte[] b) { return -1; }
    public int read(final byte[] b, final int off, final int len) { return -1;}
    public long skip(final long n) { return 0; }
    public int available() { return 0; }
    public void close() {}
    public void mark(final int readlimit) {}
    public void reset() {}
    public boolean markSupported() { return true; }
}
