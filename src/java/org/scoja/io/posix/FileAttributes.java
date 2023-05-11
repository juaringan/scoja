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

public class FileAttributes {

    public static final String NO_NAME = null;
    public static final int NO_ID = -1;
    public static final int NO_MODE = -1;

    protected final String userName;
    protected final int userID;
    protected final String groupName;
    protected final int groupID;
    protected final FileMode mode;
    
    private static FileAttributes defaultInstance;
    static {
        try {
            defaultInstance = new FileAttributes(NO_NAME, NO_NAME, NO_MODE);
        } catch (IOException e) {
            //Cannot happen
            defaultInstance = null;
        }
    }
        
    public static FileAttributes getDefault() {
        return defaultInstance;
    }
    
    public FileAttributes(final String userName,
                          final String groupName,
                          final FileMode mode)
    throws IOException {
        this.userName = userName;
        if (userName == NO_NAME) {
            this.userID = NO_ID;
        } else {
            this.userID = UserInfo.forName(userName).getID();
        }
        this.groupName = groupName;
        if (groupName == NO_NAME) {
            this.groupID = NO_ID;
        } else {
            this.groupID = GroupInfo.forName(groupName).getID();
        }
        this.mode = mode;
    }
    
    public FileAttributes(final String userName,
                          final String groupName,
                          final int mode)
    throws IOException {
        this(userName, groupName,
             (mode == NO_MODE) ? null : new FileMode(mode));
    }
    
    public FileAttributes(final String userName, final String groupName,
                          final String mode)
    throws IOException, IllegalArgumentException {
        this(userName, groupName, new FileMode(mode));
    }
    
    public String getUser() {
        return userName;
    }
    
    public int getUserID() {
        return userID;
    }
    
    public String getGroup() {
        return groupName;
    }
    
    public int getGroupID() {
        return groupID;
    }
    
    public int getMode() {
        return (mode == null) ? NO_MODE : mode.intValue();
    }
    
    public boolean hasOwnerInfo() {
        return userID != NO_ID || groupID != NO_ID;
    }
    
    public boolean hasModeInfo() {
        return mode != null;
    }
}
