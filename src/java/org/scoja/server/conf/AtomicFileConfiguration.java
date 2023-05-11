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

import java.io.File;
import java.io.IOException;

import org.scoja.common.ConfigurationException;
import org.scoja.common.PriorityUtils;
import org.scoja.server.source.Internal;

public abstract class AtomicFileConfiguration extends FileConfiguration {

    protected ConfigurationSoul soul;
    protected boolean hadErrors;
    protected boolean started;
    
    public AtomicFileConfiguration(final GlobalContext globalContext,
                                   final String filename)
        throws IOException {
        super(globalContext, filename);
        this.soul = null;
        this.hadErrors = false;
        this.started = false;
    }

    public synchronized void reload() {
        if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
            Internal.debug(Internal.CONFIG,
                           "Checking configuration file " + file.getFile());
        }
        if (!shouldReload()) return;
        Internal.warning(Internal.CONFIG,
                         "(Re)Loading configuration file " + file.getFile());
        try {
            final ConfigurationSoul newSoul = load();
            Internal.warning(Internal.CONFIG,
                             "Configuration file " + file.getFile()
                             + " successfully parsed");
            if (started) {
                if (soul != null) {
                    Internal.warning(Internal.CONFIG,
                                     "Stopping current context for "
                                     + file.getFile());
                    soul.shouldStop();
                }
                Internal.warning(Internal.CONFIG,
                                 "Starting new context for " + file.getFile());
                newSoul.start();
            }
            soul = newSoul;
            hadErrors = false;
            Internal.warning(Internal.CONFIG, "Configuration file "
                             + file.getFile() + " successfully (re)loaded");
        } catch (Throwable error) {
            //error.printStackTrace(System.err);
            Internal.err(Internal.CONFIG, "While (re)loading \""
                         + file.getFile() + "\": " + error);
            hadErrors = true;
        }
        file.updateModification();
        if (soul != null) soul.updateDependeciesModification();
    }
    
    public synchronized void start() {
        if (started) return;
        if (soul == null) reload();
        started = true;
        if (soul == null) return;
        soul.start();
    }
    
    public synchronized void stop() {
        if (!started) return;
        started = false;
        if (soul != null) soul.shouldStop();
    }
    
    public synchronized void stats() {
    }
    
    
    //======================================================================
    protected boolean shouldReload() {
        return (soul == null && !hadErrors)
            || file.hasChanged()
            || (soul != null && soul.hasDependeciesChanged());
    }

    protected abstract ConfigurationSoul load()
    throws ConfigurationException;
}
