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

import java.io.BufferedWriter;
import java.io.Writer;
import org.scoja.io.CharBuffer;
import org.scoja.io.CharStreams;

public interface WriteBufferer {
    
    public Writer buffer(Writer out);
    
    //======================================================================
    public static class Id
        implements WriteBufferer {
        
        public Writer buffer(final Writer out) {
            return out;
        }
    }
    
    
    //======================================================================
    public static class Buffering
        implements WriteBufferer {
        
        protected final int size;
        
        public Buffering(final int size) {
            this.size = size;
        }
        
        public Writer buffer(final Writer out) {
            return new BufferedWriter(out, size);
        }
    }
   
    
    //======================================================================
    public static class Delayed
        implements WriteBufferer {
        
        protected final int size;
        
        public Delayed(final int size) {
            this.size = size;
        }
        
        public Writer buffer(final Writer out) {
            final CharBuffer buffer = new CharBuffer(size);
            new Thread(new CharStreams.Copier(out, buffer.getReader())
                       .withSize(size).withClose(true)).start();
            return buffer.getWriter();
        }
    }
}
