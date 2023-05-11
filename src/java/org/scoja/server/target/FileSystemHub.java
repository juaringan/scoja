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

import java.io.File;

import org.scoja.cc.util.TrieMap;

public class FileSystemHub implements FileSystem {

    protected final TrieMap<FileSystem> fss;

    public FileSystemHub() {
        this(new LocalFileSystem());
    }
        
    public FileSystemHub(final FileSystem rootfs) {
        this.fss = new TrieMap<FileSystem>();
        this.fss.put("", rootfs);
    }
    
    public void mount(final String mountPoint, final FileSystem fs) {
        fss.put(cleaned(mountPoint), fs);
    }
    
    public void umount(final String mountPoint) {
        final String mp = cleaned(mountPoint);
        if ("".equals(mp)) {
            throw new IllegalArgumentException(
                "Root filesystem cannot be umount-ed, only redefined");
        }
        fss.remove(mp);
    }

    public TextFile openText(final String filename, 
                             final FileBuilding building, 
                             final Flushing flushing) {
        return findFileSystem(filename).openText(filename, building, flushing);
    }
    
    protected FileSystem findFileSystem(final String filename) {
        final FileSystem fs = fss.getBiggestPrefixValue(filename);
        if (fs == null) {
            throw new IllegalStateException(
                "No mountpoint is a prefix of `" + filename + "'");
        }
        return fs;
    }
    
    protected String cleaned(final String path) {
        int len = path.length();
        while (len > 0 && path.charAt(len-1) == File.separatorChar) len--;
        return path.substring(0,len);
    }
}
