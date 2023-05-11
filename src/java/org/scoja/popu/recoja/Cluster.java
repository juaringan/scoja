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

package org.scoja.popu.recoja;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.scoja.popu.common.EventSource;
import org.scoja.popu.common.FileManager;
import org.scoja.popu.common.Format;
import org.scoja.popu.common.MixingEventSource;
import org.scoja.popu.common.ReaderEventSource;

public class Cluster {

    protected final String keyfile;
    protected final Map/*<String,Format>*/ sources;
    
    public Cluster(final String keyfile) {
        this.keyfile = keyfile;
        this.sources = new HashMap();
    }
    
    public Cluster withFormat(final String source, final Format format) {
        sources.put(source, format);
        return this;
    }
    
    public Cluster withUnknownFormat(final String source) {
        sources.put(source, null);
        return this;
    }
    
    public String getKey() {
        return keyfile;
    }
    
    public boolean targetExists() {
        return new File(keyfile).exists();
    }
    
    public void deleteSources()
    throws IOException {
        StringBuffer sb = null;
        for (Iterator it = sources.keySet().iterator(); it.hasNext(); ) {
            final File file = new File((String)it.next());
            if (file.exists() && !file.delete()) {
                if (sb == null) sb = new StringBuffer("Cannot delete ");
                else sb.append(", ");
                sb.append(file);
            }
        }
        if (sb != null) throw new IOException(sb.toString());
    }
    
    public void recover(final FileManager fileManager)
    throws IOException {
        if (sources.size() == 1) recoverRenaming();
        else recoverMixing(fileManager);
    }
    
    public void recoverRenaming()
    throws IOException {
        final String origfile = (String)sources.keySet().iterator().next();
        if (!new File(origfile).renameTo(new File(keyfile))) {
            throw new IOException(
                "Cannot rename " + origfile + " to " + keyfile);
        }
    }
    
    public void recoverMixing(final FileManager fileManager)
    throws IOException {
        Writer out = null;
        final Reader[] ins = new Reader[sources.size()];
        try {
            final EventSource[] ress = new EventSource[ins.length];
            out = fileManager.openWrite(keyfile);
            final Iterator it = sources.entrySet().iterator();
            for (int i = 0; i < ins.length; i++) {
                final Map.Entry entry = (Map.Entry)it.next();
                ins[i] = fileManager.openRead((String)entry.getKey());
                ress[i] = new ReaderEventSource(
                    (Format)entry.getValue(), ins[i]);
            }
            final EventSource main = new MixingEventSource(ress);
            for (;;) {
                main.advance();
                if (!main.has()) break;
                main.current().writeTo(out);
            }
        } finally {
            if (out != null) {
                try { out.close(); } catch (Throwable e) {}
            }
            for (int i = 0; i < ins.length; i++) {
                if (ins[i] != null) {
                    try { ins[i].close(); } catch (Throwable e) {}
                }
            }
        }
    }
    
    
    //======================================================================
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("Cluster[")
            .append("\n  key: ").append(keyfile);
        int i = 0;
        for (Iterator it = sources.entrySet().iterator(); it.hasNext(); i++) {
            final Map.Entry entry = (Map.Entry)it.next();
            sb.append("\n  file ").append(i)
                .append(": ").append(entry.getKey())
                .append('[').append(entry.getValue()).append(']');
        }
        sb.append("\n]");
        return sb.toString();
    }
}
