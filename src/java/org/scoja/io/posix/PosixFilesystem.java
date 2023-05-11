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

import java.io.IOException;
import java.io.FileDescriptor;

/**
 * Posix services that involve a filesystem but no IO (see {@link PosixIO}).
 *
 * @see PosixFilesystem
 * @see PosixIO
 */
public interface PosixFilesystem {

    public boolean hasFilesystem();
    
    //======================================================================
    public static final int KEEP_USER = -1;
    public static final int KEEP_GROUP = -1;

    //======================================================================
    public FileStat getFileStat(String filename)
    throws IOException;
    
    public FileStat getFileStat(FileDescriptor fileid)
    throws IOException;
    
    public void setFileMode(String filename, int mode)
    throws IOException;
        
    public void setFileMode(FileDescriptor fileid, int mode)
    throws IOException;
        
    public void setFileOwner(String filename, int userid, int groupid)
    throws IOException;
        
    public void setFileOwner(FileDescriptor fileid, int userid, int groupid)
    throws IOException;
}
