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
 * Basic information of a Posix system group.
 * <p>
 * Equality is fully structural: all fields must be equal for two
 * {@link GroupInfo}s to be equal.
 */
public class GroupInfo {

    protected final String name;
    protected final int gid;
    protected final String[] members;
    
    public static GroupInfo getCurrent()
    throws IOException {
        return forID(Posix.getPosix().getCurrentGroup());
    }
    
    public static GroupInfo getEffective()
    throws IOException {
        return forID(Posix.getPosix().getEffectiveGroup());
    }
    
    public static GroupInfo forName(final String name)
    throws IOException {
        final GroupInfo result = Posix.getPosix().getGroupInfo(name);
        if (result == null) {
            throw new IOException("Unknown group \"" + name + "\"");
        }
        return result;
    }
    
    public static GroupInfo forID(final int id)
    throws IOException {
        final GroupInfo result = Posix.getPosix().getGroupInfo(id);
        if (result == null) {
            throw new IOException("Unknown group id " + id);
        }
        return result;
    }
    
    protected GroupInfo(final String name,
                        final int gid,
                        final String[] members) {
        this.name = name;
        this.gid = gid;
        this.members = members;
    }
    
    protected GroupInfo(final byte[] name,
                        final int gid,
                        final byte[][] members) {
        this.name = new String(name);
        this.gid = gid;
        this.members = new String[members.length];
        for (int i = 0; i < members.length; i++) {
            this.members[i] = new String(members[i]);
        }
    }
    
    public String getName() {
        return name;
    }
    
    public int getID() {
        return gid;
    }
    
    public String[] getMembers() {
        return members;
    }
    
    
    //======================================================================
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("GroupInfo[")
            .append("name: ").append(name)
            .append(", id: ").append(gid)
            .append(", members:");
        for (int i = 0; i < members.length; i++) {
            sb.append(' ').append(members[i]);
        }
        sb.append(']');
        return sb.toString();
    }
    
    public int hashCode() {
        return gid;
    }
    
    public boolean equals(final Object other) {
        return (other instanceof GroupInfo)
            && equals((GroupInfo)other);
    }
    
    public boolean equals(final GroupInfo other) {
        return other != null
            && this.getID() == other.getID()
            && this.getName().equals(other.getName())
            && sameMembers(this,other);
    }
    
    private static boolean sameMembers(final GroupInfo g1, final GroupInfo g2){
        final String[] m1 = g1.getMembers();
        final String[] m2 = g2.getMembers();
        if (m1.length != m2.length) return false;
        for (int i = 0; i < m1.length; i++) {
            if (!m1[i].equals(m2[i])) return false;
        }
        return true;
    }
}
