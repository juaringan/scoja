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

package org.scoja.popu.common;

import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.scoja.io.ReadBufferer;
import org.scoja.io.WriteBufferer;

public class FileManager {

    protected final List/*<ReadOpener>*/ readOpeners;
    protected ReadBufferer readBufferer;
    
    protected final List/*<WriteOpener>*/ writeOpeners;
    protected WriteBufferer writeBufferer;
    
    public FileManager() {
        this.readOpeners = new ArrayList();
        this.readBufferer = new ReadBufferer.Id();
        this.writeOpeners = new ArrayList();
        this.writeBufferer = new WriteBufferer.Id();
    }

    public Reader openRead(final String filename)
    throws IOException {
        Reader in = null;
        for (Iterator it = readOpeners.iterator(); 
             in == null && it.hasNext(); ) {
            in = ((ReadOpener)it.next()).open(filename);
        }
        if (in == null) in = new FileReader(filename);
        
        in = readBufferer.buffer(in);
        
        return in;
    }
    
    public Writer openWrite(final String filename)
    throws IOException {
        Writer out = null;
        for (Iterator it = writeOpeners.iterator(); 
             out == null && it.hasNext(); ) {
            out = ((WriteOpener)it.next()).open(filename);
        }
        if (out == null) out = new FileWriter(filename);
        
        out = writeBufferer.buffer(out);
        
        return out;
    }
    
    public void installReadOpener(final ReadOpener opener) {
        readOpeners.add(opener);
    }
    
    public void installWriteOpener(final WriteOpener opener) {
        writeOpeners.add(opener);
    }
    
    public void installReadBufferer(final ReadBufferer bufferer) {
        readBufferer = bufferer;
    }
    
    public void installWriteBufferer(final WriteBufferer bufferer) {
        writeBufferer = bufferer;
    }
    
    public void manageExtension(final String ext) {
        if (".gz".equals(ext) || "gz".equals(ext)) {
            installReadOpener(new ReadOpener.ForGZip());
            installWriteOpener(new WriteOpener.ForGZip());
        } else {
            throw new IllegalArgumentException(
                "Cannot manage extension " + ext);
        }
    }
}
