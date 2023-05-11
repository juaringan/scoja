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

package org.scoja.server.target;

import java.io.*;

import org.scoja.cc.util.Sizer;

import org.scoja.util.ExpiringLRUCache;
import org.scoja.util.LRUShell;

import org.scoja.server.core.CPUUsage;
import org.scoja.server.core.EventContext;
import org.scoja.server.core.Link;
import org.scoja.server.source.Internal;
import org.scoja.server.template.EventWriter;
import org.scoja.server.template.Template;

/**
 * Es la clase común para todos los destinos de ficheros.
 */
public abstract class FileTarget extends Link {
    
    protected final FileSystem fileSystem;
    protected final ExpiringLRUCache fileCache;
    protected final Sizer sizer;
    protected EventWriter writer;
    protected FileBuilding building;
    protected Flushing flushing;
    protected FilesMeasurable stats;
    
    public FileTarget(final FileSystem fileSystem,
                      final ExpiringLRUCache fileCache) {
        this.fileSystem = fileSystem;
        this.fileCache = fileCache;
        this.sizer = new Sizer.BoundedDoubling(128, 0.25);
        this.writer = EventWriter.Standard.getInstance();
        this.building = FileBuilding.getDefault();
        this.flushing = Flushing.getDefault();
        this.stats = NullFilesMeasurable.getInstance();
    }
    
    public void setFormat(final Template writer) {
        this.writer = writer;
    }
    
    public void setBuilding(final FileBuilding building) {
        this.building = building;
    }
    
    public void setFlushing(final Flushing flushing) {
        this.flushing = flushing;
    }
    
    public void setStats(final FilesMeasurable stats) {
        this.stats = stats;
    }
    
    public void process(final EventContext env) {
        write(env);
        propagate(env);
    }

    protected abstract String getFilename(EventContext ectx);

    protected void write(final EventContext ectx) {
        final String filename = getFilename(ectx);
        final int size;
        synchronized (sizer) { size = sizer.suggestedSize(); }
        final CharArrayWriter buffer = new CharArrayWriter(size);
        final PrintWriter printer = new PrintWriter(buffer);
        writer.writeTo(printer, ectx);
        printer.flush();
        synchronized (sizer) { sizer.foundSize(buffer.size()); }
        LRUShell fileShell = null;
        try {
            try {
                boolean opened = false;
                fileShell = fileCache.get(filename);
                TextFile tf = (TextFile)fileShell.getValue();
                if (tf == null) {
                    tf = fileSystem.openText(filename, building, flushing);
                    fileShell.put(tf);
                    opened = true;
                }
                //This measure is so expensive and the mesaured code is so
                // cheap, that enabling it makes the server increase by
                // 3 or 4 times its CPU consumption.
                //final CPUUsage t0 = CPUUsage.forCurrentThread();
                synchronized (tf) {
                    buffer.writeTo(tf.getOut());
                    tf.event();
                    flushing.flush(tf);
                }
                //final CPUUsage t1 = CPUUsage.forCurrentThread();
                //t1.dec(t0);
                stats.written(filename, opened, 1, buffer.size(), /*t1*/ null);
            } finally {
                if (fileShell != null) fileShell.release();
            }
        } catch (Throwable e) {
            Internal.crit(ectx, Internal.TARGET_FILE,
                          "Error " + e.getMessage()
                          + " while writing event " + ectx.getEvent());
        }
    }
}
