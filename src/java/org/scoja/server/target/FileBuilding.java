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

import org.scoja.io.posix.FileAttributes;

public class FileBuilding {

    protected final FileAttributes fileAttributes;
    protected final FileAttributes directoryAttributes;
    protected final boolean makeDirectories;
    
    private static final FileBuilding defaultInstance
        = new FileBuilding(FileAttributes.getDefault(),
                           FileAttributes.getDefault(),
                           true);
                           
    public static FileBuilding getDefault() {
        return defaultInstance;
    }
    
    public FileBuilding(final FileAttributes fileAttributes,
                        final FileAttributes directoryAttributes,
                        final boolean makeDirectories) {
        this.fileAttributes = fileAttributes;
        this.directoryAttributes = directoryAttributes;
        this.makeDirectories = makeDirectories;
    }
    
    public FileAttributes getFileAttributes() {
        return fileAttributes;
    }
    
    public FileAttributes getDirectoryAttributes() {
        return directoryAttributes;
    }
    
    public boolean shouldMakeDirectories() {
        return makeDirectories;
    }
}
