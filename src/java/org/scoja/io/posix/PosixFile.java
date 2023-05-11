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

package org.scoja.io.posix;

import java.io.File;
import java.io.IOException;

/**
 * An extended {@link File} to cope with Posix capability.
 *
 * @todo
 * Methods
 * {@link #listFiles()},
 * {@link #listFiles(FileFilter)},
 * and {@link #listFiles(FilenameFilter)}
 * currently return a {@link File} instead of a PosixFile.
 */
public class PosixFile
    extends File {

    protected FileAttributes defaultAttributes;

    public PosixFile(final String pathname) {
        this(pathname, FileAttributes.getDefault());
    }
    
    public PosixFile(final String pathname,
                     final FileAttributes defaultAttributes) {
        super(pathname);
        this.defaultAttributes = defaultAttributes;
    }
    
    public PosixFile(final File file) {
        this(file.getPath());
    }
    
    public PosixFile(final File file,
                     final FileAttributes defaultAttributes) {
        this(file.getPath(), defaultAttributes);
    }

    //----------------------------------------------------------------------
    public boolean isFileType(final int fileType)
    throws IOException {
        return FileStat.forFile(this).getFileMode().isFileType(fileType);
    }
    
    public boolean isSocket()
    throws IOException {
        return isFileType(FileMode.IFSOCK);
    }
    
    public boolean isLink()
    throws IOException {
        return isFileType(FileMode.IFLNK);
    }
    
    public boolean isBlock()
    throws IOException {
        return isFileType(FileMode.IFBLK);
    }
    
    public boolean isCharacterDevice()
    throws IOException {
        return isFileType(FileMode.IFCHR);
    }
    
    public boolean isFifo()
    throws IOException {
        return isFileType(FileMode.IFIFO);
    }
        
    //----------------------------------------------------------------------
    public void setDefaultAttributes(final FileAttributes fa) {
        this.defaultAttributes = fa;
    }
    
    public void writeAttributes() throws IOException {
        final PosixLike posix = Posix.getPosix();
        if (defaultAttributes.hasOwnerInfo()) {
            posix.setFileOwner(
                getPath(),
                defaultAttributes.getUserID(),
                defaultAttributes.getGroupID());
        }
        if (defaultAttributes.hasModeInfo()) {
            posix.setFileMode(getPath(), defaultAttributes.getMode());
        }
    }
    
    public boolean createNewFile() throws IOException {
        final boolean created = super.createNewFile();
        if (created) writeAttributes();
        return created;
    }
    
    public File getAbsoluteFile() {
        return new PosixFile(getAbsolutePath(), defaultAttributes);
    }
    
    public File getCanonicalFile() throws IOException {
        return new PosixFile(getCanonicalPath(), defaultAttributes);
    }
    
    public File getParentFile() {
        final String parent = getParent();
        if (parent == null) return null; 
        else return new PosixFile(parent, defaultAttributes);
    }

    public boolean makeDirectory() throws IOException {
        final boolean created = super.mkdir();
        if (created) writeAttributes();
        return created;
    }

    public boolean makeDirectories() throws IOException {
        return new PosixFile(getCanonicalPath(), defaultAttributes)
            .makeDirectories0();
    }
    
    private boolean makeDirectories0() throws IOException {
        if (exists()) return false;
	String parent = getParent();
        if (parent != null) {
            new PosixFile(parent, defaultAttributes).makeDirectories0();
        }
        return makeDirectory();
    }
}
