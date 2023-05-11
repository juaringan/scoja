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

/**
 * Basic information of a Posix system user.
 * <p>
 * Equality is fully structural: all fields must be equal for two
 * {@link UserInfo}s to be equal.
 */
public class UserInfo {

    protected final String name;
    protected final int uid;
    protected final int gid;
    protected final String realName;
    protected final String homeDirectory;
    protected final String shell;
    
    public static UserInfo getCurrent()
    throws IOException {
        return forID(Posix.getPosix().getCurrentUser());
    }
    
    public static UserInfo getEffective()
    throws IOException {
        return forID(Posix.getPosix().getEffectiveUser());
    }
    
    public static UserInfo forName(final String name)
    throws IOException {
        final UserInfo result = Posix.getPosix().getUserInfo(name);
        if (result == null) {
            throw new IOException("Unknown user \"" + name + "\"");
        }
        return result;
    }
    
    public static UserInfo forID(final int id) 
    throws IOException {
        final UserInfo result = Posix.getPosix().getUserInfo(id);
        if (result == null) {
            throw new IOException("Unknown user id " + id);
        }
        return result;
    }
    
    protected UserInfo(final String name,
                       final int uid,
                       final int gid,
                       final String realName,
                       final String homeDirectory,
                       final String shell) {
        this.name = name;
        this.uid = uid;
        this.gid = gid;
        this.realName = realName;
        this.homeDirectory = homeDirectory;
        this.shell = shell;
    }
    
    protected UserInfo(final byte[] name,
                       final int uid,
                       final int gid,
                       final byte[] realName,
                       final byte[] homeDirectory,
                       final byte[] shell) {
        this(new String(name), uid, gid,
             new String(realName), new String(homeDirectory),
             new String(shell));
    }
    
    public String getName() {
        return name;
    }
    
    public int getID() {
        return uid;
    }
    
    public int getGroupID() {
        return gid;
    }
    
    public String getRealName() {
        return realName;
    }
    
    public String getHomeDirectory() {
        return homeDirectory;
    }
    
    public String getShell() {
        return shell;
    }
    
    
    //======================================================================
    public String toString() {
        return "UserInfo["
            + "name: " + name
            + ", uid: " + uid
            + ", gid: " + gid
            + ", real name: " + realName
            + ", home: " + homeDirectory
            + ", shell: " + shell
            + "]";
    }
    
    public int hashCode() {
        return uid;
    }
    
    public boolean equals(final Object other) {
        return (other instanceof UserInfo)
            && equals((UserInfo)other);
    }
    
    public boolean equals(final UserInfo other) {
        return other != null
            && this.getID() == other.getID()
            && this.getName().equals(other.getName())
            && this.getGroupID() == other.getGroupID()
            && this.getRealName().equals(other.getRealName())
            && this.getHomeDirectory().equals(other.getHomeDirectory())
            && this.getShell().equals(other.getShell());
    }
}
