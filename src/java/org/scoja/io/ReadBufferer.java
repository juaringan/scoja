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

import java.io.BufferedReader;
import java.io.Reader;

public interface ReadBufferer {
    
    public Reader buffer(Reader in);
    
    //======================================================================
    public static class Id
        implements ReadBufferer {
        
        public Reader buffer(final Reader in) {
            return in;
        }
    }
    
    
    //======================================================================
    public static class Buffering
        implements ReadBufferer {
        
        protected final int size;
        
        public Buffering(final int size) {
            this.size = size;
        }
        
        public Reader buffer(final Reader in) {
            return new BufferedReader(in, size);
        }
    }
    
    
    //======================================================================
    public static class Ahead
        implements ReadBufferer {
        
        protected final int size;
        
        public Ahead(final int size) {
            this.size = size;
        }
        
        public Reader buffer(final Reader in) {
            final CharBuffer buffer = new CharBuffer(size);
            new Thread(new CharStreams.Copier(buffer.getWriter(), in)
                       .withSize(size).withClose(true)).start();
            return buffer.getReader();
        }
    }
}
