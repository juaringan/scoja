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

package org.scoja.popu.recoja;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.scoja.popu.common.FileManager;
import org.scoja.popu.common.Format;
import org.scoja.popu.common.FormatRule;

/**
 * This is the main computational class of Recoja.
 */
public class Recoverer {

    private static final Logger log
        = Logger.getLogger(Recoverer.class.getName());

    protected final RecoveringReport rr;
    protected final boolean notDirIsFile;
    protected final List clusterRules;
    protected final List formatRules;
    protected final Map/*<String,Cluster>*/ clusters;
    protected final Collection unclustered;
    
    public Recoverer(final RecoveringReport rr, final boolean notDirIsFile) {
        this.rr = rr;
        this.notDirIsFile = notDirIsFile;
        this.clusterRules = new ArrayList();
        this.formatRules = new ArrayList();
        this.clusters = new HashMap();
        this.unclustered = new ArrayList();
    }
    
    protected void reset() {
        clusterRules.clear();
        formatRules.clear();
        clusters.clear();
    }
    
    public void cluster(final RewritingRule rr) {
        clusterRules.add(rr);
    }
    
    public void parseWith(final FormatRule fr) {
        formatRules.add(fr);
    }
    
    public void consider(final String fileordir) {
        try {
            final File fd = new File(fileordir).getCanonicalFile();
            consider(fd);
        } catch (IOException e) {
            log.log(Level.WARNING, "Error while considering " + fileordir, e);
        }
    }
    
    public void consider(final File fd) {
        if (fd.isDirectory()) {
            considerDirectory(fd);
        } else if (notDirIsFile || fd.isFile()) {
            considerFile(fd);
        } else {
            log.log(Level.INFO, "Avoiding " + fd
                    + ": neither a directory nor a regular file");
        }
    }
    
    protected void considerDirectory(final File dir) {
        final File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            consider(files[i]);
        }
    }
    
    protected void considerFile(final File file) {
        considerFile(file.toString());
    }
    
    protected void considerFile(final String file) {
        final Cluster cluster = clusterFor(file);
        //System.out.println("Cluster for " + file + ": " + cluster);
        if (cluster == null) {
            unclustered.add(file);
        } else {
            final Format format = formatFor(file);
            //System.out.println("Format for " + file + ": " + format);
            if (format != null) cluster.withFormat(file, format);
            else cluster.withUnknownFormat(file);
        }
    }
    
    protected Cluster clusterFor(final String origfile) {
        final String keyfile = keyFileFor(origfile);
        if (keyfile == null) return null;
        Cluster cluster = (Cluster)clusters.get(keyfile);
        if (cluster == null) {
            cluster = new Cluster(keyfile);
            clusters.put(keyfile, cluster);
        }
        return cluster;
    }
    
    public String keyFileFor(final String origfile) {
        String keyfile = null;
        for (Iterator it = clusterRules.iterator();
             it.hasNext() && keyfile == null; ) {
            keyfile = ((RewritingRule)it.next()).rewrite(origfile);
        }
        return keyfile;
    }
    
    public Format formatFor(final String origfile) {
        Format format = null;
        for (Iterator it = formatRules.iterator();
             it.hasNext() && format == null; ) {
            format = ((FormatRule)it.next()).apply(origfile);
        }
        return format;
    }
    
    public void inform(final PrintWriter out) {
        informFormats(out);
        informClusterRules(out);
        informClusters(out);
        informUnclustered(out);
        out.flush();
    }
    
    protected void informFormats(final PrintWriter out) {
        out.println("FORMATS");
        for (Iterator it = formatRules.iterator(); it.hasNext(); ) {
            out.println(it.next());
        }
    }
    
    protected void informClusterRules(final PrintWriter out) {
        out.println("CLUSTER RULES");
        for (Iterator it = clusterRules.iterator(); it.hasNext(); ) {
            out.println(it.next());
        }
    }
    
    protected void informClusters(final PrintWriter out) {
        out.println("CLUSTERS");
        for (Iterator it = clusters.values().iterator(); it.hasNext(); ) {
            out.println(it.next());
        }
    }
    
    protected void informUnclustered(final PrintWriter out) {
        out.println("UNCLUSTERED FILES");
        for (Iterator it = unclustered.iterator(); it.hasNext(); ) {
            out.println(it.next());
        }
    }
    
    public void recover(final FileManager fileManager) {
        for (Iterator it = clusters.values().iterator(); it.hasNext(); ) {
            final Cluster cluster = (Cluster)it.next();
            log.log(Level.INFO, "Recovering " + cluster.getKey());
            try {
                cluster.recover(fileManager);
                cluster.deleteSources();
            } catch (IOException e) {
                log.log(Level.SEVERE, 
                        "Error while recovering " + cluster.getKey(), e);
            }
        }
    }
}
