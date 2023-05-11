/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2008  Bankinter, S.A.
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

package org.scoja.server.target;

import java.io.IOException;
import java.io.Writer;

public interface TextFile {

    public Writer getOut();
    
    public void event();
    
    public void flush(int whenUnflushedEvents);
    
    public void sync(int whenUnsyncEvents);
    
    public void close();
    
    
    //======================================================================
    public static class Canonical implements TextFile {
        protected final BinaryFile bfile;
        protected final Writer writer;
        protected long events;
        protected int unevents;
        
        public Canonical(final BinaryFile bfile, final Writer writer) {
            this.bfile = bfile;
            this.writer = writer;
            this.events = 0;
            this.unevents = 0;
        }
        
        public Writer getOut() { return writer; }
        
        public void event() { unevents++; }
        
        public void flush(final int whenUnflushedEvents) {
            if (unevents >= whenUnflushedEvents) {
                flush();
            }
        }

        public void flush() {
            try {
                writer.flush();
                resetEvents();
            } catch (IOException cannotHappen) {
                //Because OutputStream of a BinaryFile never throws
                // IOExceptions.
            }
        }
        
        public void sync(final int whenUnsyncEvents) {
            if (unevents >= whenUnsyncEvents) {
                try {
                    writer.flush();
                    bfile.sync();
                    resetEvents();
                } catch (IOException cannotHappen) {
                   //Because OutputStream of a BinaryFile never throws
                    // IOExceptions.
                }
             }
        }
        
        protected void resetEvents() {
            events += unevents;
            unevents = 0;
        }
        
        public void close() {
            flush();
            bfile.close();
        }
        
        public String toString() {
            return "TextFile[" + bfile + "]";
        }
    }
}
