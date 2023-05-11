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

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

import org.scoja.server.core.Cluster;
import org.scoja.server.core.HubMeasurable;
import org.scoja.server.core.Link;
import org.scoja.server.source.Measurer;
import org.scoja.server.target.FileLRUCache;

public class ConfigurationSoul {

    protected final GlobalContext globalContext;
    protected final Link localInternal;
    protected final Link localMeasurerLink;
    protected final HubMeasurable localMeasurerHub;
    protected final List clusters;
    protected final List dependencies;
    
    protected boolean started;
    protected boolean stopRequested;

    public ConfigurationSoul(final GlobalContext globalContext) {
        this.globalContext = globalContext;
        this.localInternal = new Link();
        this.localMeasurerLink = new Link();
        this.localMeasurerHub = new HubMeasurable();
        this.clusters = new ArrayList();
        this.dependencies = new ArrayList();
        this.started = false;
        this.stopRequested = false;
    }
    
    public Link getInternal() {
        return localInternal;
    }
    
    public Link getMeasurerLink() {
        return localMeasurerLink;
    }
    
    public HubMeasurable getMeasurerHub() {
        return localMeasurerHub;
    }
    
    public GlobalContext getGlobalContext() {
        return globalContext;
    }
    
    public synchronized void addCluster(final Cluster cluster) {
        clusters.add(cluster);
        if (started && !stopRequested) {
            cluster.start();
        }
    }
    
    public synchronized void addDependency(final File file) {
        dependencies.add(new Dependency(file));
    }
    
    public synchronized boolean hasDependeciesChanged() {
        final Iterator it = dependencies.iterator();
        while (it.hasNext()) {
            if (((Dependency)it.next()).hasChanged()) return true;
        }
        return false;
    }
    
    public synchronized void updateDependeciesModification() {
        final Iterator it = dependencies.iterator();
        while (it.hasNext()) {
            ((Dependency)it.next()).updateModification();
        }
    }
    
    public synchronized void start() {
        if (started && !stopRequested) return;
        globalContext.getInternal().addTarget(localInternal);
        final Measurer measurer = globalContext.getMeasurer();
        measurer.addTarget(localMeasurerLink);
        measurer.addMeasurable(localMeasurerHub);
        final Iterator it = clusters.iterator();
        while (it.hasNext()) {
            ((Cluster)it.next()).start();
        }
        started = true;
        stopRequested = false;
    }
    
    public synchronized void shouldStop() {
        if (!started || stopRequested) return;
        final Iterator it = clusters.iterator();
        while (it.hasNext()) {
            ((Cluster)it.next()).shouldStop();
        }
        globalContext.getInternal().removeTarget(localInternal);
        final Measurer measurer = globalContext.getMeasurer();
        measurer.removeTarget(localMeasurerLink);
        measurer.removeMeasurable(localMeasurerHub);
        stopRequested = true;
    }
}
