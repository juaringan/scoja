/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
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

package org.scoja.io;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * A utility class to work with char streams
 * ({@link Reader}s and {@link Writer}s).
 */
public class CharStreams {

    public static final int COPY_BUFFER_DEFAULT_SIZE = 4*1024;

    public static void copy(final Writer out, final Reader in, final int size)
    throws IOException {
        final char[] buffer = new char[size];
        for (;;) {
            final int read = in.read(buffer);
            if (read == -1) break;
            out.write(buffer, 0, read);
        }
    }
    
    public static void copy(final Writer out, final Reader in)
    throws IOException {
        copy(out, in, COPY_BUFFER_DEFAULT_SIZE);
    }
    
    //======================================================================
    public static class Copier
        implements Runnable {
        
        protected final Writer out;
        protected final Reader in;
        protected int size;
        protected boolean targetClose;
        
        public Copier(final Writer out, final Reader in) {
            this.out = out;
            this.in = in;
            this.size = COPY_BUFFER_DEFAULT_SIZE;
            this.targetClose = false;
        }
        
        public Copier withSize(final int size) {
            this.size = size;
            return this;
        }
        
        public Copier withClose(final boolean close) {
            this.targetClose = close;
            return this;
        }
        
        public void run() {
            try {
                copy();
            } catch (IOException e) {
                //FIXME: Log
                e.printStackTrace(System.err);
            }
        }
        
        public void copy()
        throws IOException {
            try {
                CharStreams.copy(out, in, size);
            } finally {
                if (targetClose) out.close();
            }
        }
    }
}
