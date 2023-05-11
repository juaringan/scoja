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
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.scoja.common.PriorityUtils;
import org.scoja.server.source.Internal;

public class GroupConfiguration extends FileConfiguration {

    protected List entries;
    protected Map confs;
    protected boolean loaded;
    protected boolean started;

    public GroupConfiguration(final GlobalContext globalContext,
                              final String filename)
        throws IOException {
        super(globalContext, filename);
        this.entries = new ArrayList();
        this.confs = new HashMap();
        this.loaded = false;
        this.started = false;
    }
    
    public void reload() {
        if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
            Internal.debug(Internal.CONFIG,
                           "Checking configuration file " + file.getFile());
        }
        if (loaded && !file.hasChanged()) return;
        Internal.warning(Internal.CONFIG,
                         "(Re)Loading configuration file " + file.getFile());
        final File base = file.getFile().getParentFile();
        final Map current = new HashMap(confs);
        entries.clear();
        confs.clear();
        try {
            final BufferedReader in
                = new BufferedReader(new FileReader(file.getFile()));
            for (;;) {
                final String line = in.readLine();
                if (line == null) break;
                
                final int hash = line.indexOf('#');
                String useful;
                if (hash == -1) useful = line;
                else useful = line.substring(0, hash);
                useful = useful.trim();
                if (useful.length() == 0) continue;
                
                final int split = first(useful.indexOf(' '),
                                        useful.indexOf('\t'));
                final String type;
                String name;
                if (split > 0) {
                    type = useful.substring(0,split).toLowerCase();
                    name = useful.substring(split+1).trim();
                } else {
                    type = "j";
                    name = useful;
                }
                File subfile = new File(name);
                if (!subfile.isAbsolute()) subfile = new File(base, name);
                name = subfile.getCanonicalPath();
                //System.err.println("Type: " + type + ", name: " + name);
                
                Configuration conf = (Configuration)current.get(name);
                boolean isNew = false;
                if ("j".equals(type) || "scoja".equals(type)) {
                    if (!(conf instanceof ScojaConfiguration)) conf = null;
                    else current.remove(name);
                    if (conf == null) {
                        isNew = true;
                        conf = new ScojaConfiguration(globalContext, name);
                    }
                } else if ("g".equals(type) || "group".equals(type)) {
                    if (!(conf instanceof GroupConfiguration)) conf = null;
                    else current.remove(name);
                    if (conf == null) {
                        isNew = true;
                        conf = new GroupConfiguration(globalContext, name);
                    }
                } else {
                    Internal.err(Internal.CONFIG,
                                 "While (re)loading configuration file "
                                 + file.getFile() + ": "
                                 + "Unknown type " + type + "; "
                                 + "file " + name + " will be ignored.");
                    continue;
                }
                conf.reload();
                if (isNew && started) conf.start();
                entries.add(name);
                confs.put(name, conf);
            }
            Internal.warning(Internal.CONFIG, "Configuration file "
                             + file.getFile() + " successfully (re)loaded");
        } catch (IOException error) {
            //error.printStackTrace(System.err);
            Internal.err(Internal.CONFIG, "While (re)loading \""
                         + file.getFile() + "\": " + error);
        }
        
        final Iterator it = current.values().iterator();
        while (it.hasNext()) {
            final Configuration conf = (Configuration)it.next();
            conf.stop();
        }
        
        loaded = true;
        file.updateModification();
    }
    
    public void start() {
        if (started) return;
        started = true;
        final Iterator it = entries.iterator();
        while (it.hasNext()) {
            final Configuration conf = (Configuration)confs.get(it.next());
            conf.start();
        } 
    }
    
    public void stop() {
        if (!started) return;
        started = false;
        final Iterator it = entries.iterator();
        while (it.hasNext()) {
            final Configuration conf = (Configuration)confs.get(it.next());
            conf.stop();
        } 
    }
    
    public void stats() {
    }
    
    //======================================================================
    private int first(final int a, final int b) {
        if (a < 0) return b;
        else if (b < 0) return a;
        else return Math.min(a,b);
    }
}
