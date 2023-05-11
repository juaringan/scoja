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

package org.scoja.server.conf;

import java.util.Timer;

import org.scoja.util.ExpiringLRUCache;
import org.scoja.util.Graveyard;
import org.scoja.server.core.Link;
import org.scoja.server.source.Internal;
import org.scoja.server.source.Measurer;
import org.scoja.server.target.FileLRUCache;
import org.scoja.server.target.FilesMeasurable;
import org.scoja.server.target.FileSystemHub;
import org.scoja.server.target.TextFile;

public class GlobalContext {

    public static final int DEFAULT_OPEN_MAX_SIZE = 500;
    public static final long DEFAULT_OPEN_MAX_INACTIVITY = 5*60*1000;

    protected final Link internal;
    protected final Measurer measurer;
    protected final FilesMeasurable fileMeasures;
    protected final Timer timer;
    protected final FileSystemHub fileSystem;
    protected final ExpiringLRUCache fileCache;
    
    public GlobalContext(final Link internal, final Measurer measurer,
                         final FilesMeasurable fileMeasures) {
        this.internal = internal;
        this.measurer = measurer;
        this.fileMeasures = fileMeasures;
        this.timer = new Timer(false);
        this.fileSystem = new FileSystemHub();
        this.fileCache = new ExpiringLRUCache(
            DEFAULT_OPEN_MAX_SIZE, DEFAULT_OPEN_MAX_INACTIVITY);
        this.fileCache.setGraveyard(new Graveyard() {
                public void died(final Object key, final Object value) {
                    //FIXME: Calling Internal at this method is very dangerous
                    // if Internal hasn't its own queue/thread.
                    //UPDATE: Really? ExpiringLRUCache seems having no problems
                    // with a reentrance due to this call.
                    Internal.notice(Internal.TARGET_FILE,
                                    "File \"" +key+ "\""
                                    + " removed from the open files cache");
                    if (value != null) ((TextFile)value).close();
                }
            });
    }
    
    public void close() {
        timer.cancel();
        fileCache.close();
    }
    
    public Link getInternal() {
        return internal;
    }
    
    public Measurer getMeasurer() {
        return measurer;
    }
    
    public FilesMeasurable getFileMeasures() {
        return fileMeasures;
    }
    
    public Timer getTimer() {
        return timer;
    }
    
    public FileSystemHub getFileSystem() {
        return fileSystem;
    }
    
    public ExpiringLRUCache getFileCache() {
        return fileCache;
    }
}
