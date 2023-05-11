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
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Date;

public class FileStat {

    public static FileStat forName(final String filename)
        throws IOException {
        return Posix.getPosix().getFileStat(filename);
    }
    
    public static FileStat forFile(final File file)
        throws IOException {
        return forName(file.toString());
    }
    
    public static FileStat forFD(final FileDescriptor fd)
        throws IOException {
        return Posix.getPosix().getFileStat(fd);
    }
    
    protected final int device;
    protected final long inode;
    protected final int mode;
    protected final int hardLinks;
    protected final int userID;
    protected final int groupID;
    protected final int deviceType;
    protected final long size;
    protected final long blockSize;
    protected final long blocksAllocated;
    protected final long lastAccess;
    protected final long lastModification;
    protected final long lastChange;
    
    protected FileStat(final int device,
                       final long inode,
                       final int mode,
                       final int hardLinks,
                       final int userID,
                       final int groupID,
                       final int deviceType,
                       final long size,
                       final long blockSize,
                       final long blocksAllocated,
                       final long lastAccess,
                       final long lastModification,
                       final long lastChange) {
        this.device = device;
        this.inode = inode;
        this.mode = mode;
        this.hardLinks = hardLinks;
        this.userID = userID;
        this.groupID = groupID;
        this.deviceType = deviceType;
        this.size = size;
        this.blockSize = blockSize;
        this.blocksAllocated = blocksAllocated;
        this.lastAccess = 1000*lastAccess;
        this.lastModification = 1000*lastModification;
        this.lastChange = 1000*lastChange;
    }

    public int getDevice() {
        return this.device;
    }
    
    public long getINode() {
        return this.inode;
    }
    
    public int getMode() {
        return this.mode;
    }
    
    public FileMode getFileMode() {
        return new FileMode(this.mode);
    }
    
    public int getHardLinks() {
        return this.hardLinks;
    }
    
    public int getUserID() {
        return this.userID;
    }
    
    public int getGroupID() {
        return this.groupID;
    }
    
    public int getDeviceType() {
        return this.deviceType;
    }
    
    public long getSize() {
        return this.size;
    }
    
    public long getBlockSize() {
        return this.blockSize;
    }
    
    public long getBlocksAllocated() {
        return this.blocksAllocated;
    }
    
    public long getLastAccessMillis() {
        return this.lastAccess;
    }
    
    public long getLasModificationMillis() {
        return this.lastModification;
    }
    
    public long getLastChangeMillis() {
        return this.lastChange;
    }
    
    public Date getLastAccess() {
        return new Date(this.lastAccess);
    }
    
    public Date getLastModification() {
        return new Date(this.lastModification);
    }
    
    public Date getLastChange() {
        return new Date(this.lastChange);
    }
    
    //======================================================================
    public String toString() {
        return "FileStat["
            + "device: " + device
            + ", inode: " + inode
            + ", mode: " + Integer.toOctalString(mode)
            + ", hard links: " + hardLinks
            + ", user id: " + userID
            + ", group id: " + groupID
            + ", device type: " + deviceType
            + ", size: " + size
            + ", block size: " + blockSize
            + ", blocks allocated: " + blocksAllocated
            + ", last access: " + getLastAccess()
            + ", last modification: " + getLastModification()
            + ", last change: " + getLastChange()
            + "]";
    }
}
