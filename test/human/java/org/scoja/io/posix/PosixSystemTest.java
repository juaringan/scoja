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

import java.io.*;

/**
 * This test shows information collected with the
 * {@link org.scoja.io.posix.PosixSystem}
 * of the {@link org.scoja.io.posix.PosixLike} class parameter.
 * This test uses {@link org.scoja.io.posix.PosixSystem} directly;
 * {@link UserGroupInfoTest} do just the same but indirectly through the
 * more abstract {@link org.scoja.io.posix.UserInfo}
 * or {@link org.scoja.io.posix.GroupInfo} classes.
 */
public class PosixSystemTest {

    public static void main(final String[] args) throws Throwable {
        if (args.length != 1) {
            System.err.println(
                "java " + PosixSystemTest.class.getName()
                + " PosixLike"
                + "\nPosixLike:"
                + "\n    " + PosixFree.class.getName()
                + "\n    " + PosixNative.class.getName());
            System.exit(-1);
        }            
        
        int argc = 0;
        final String posixLike = args[argc++];
        
        Posix.setPosix(posixLike);
        final PosixSystem ps = Posix.getPosix();
        
        System.out.print(
            "Current user id: " + ps.getCurrentUser()
            + "\nEffective user id: " + ps.getEffectiveUser()
            + "\nCurrent group id: " + ps.getCurrentGroup()
            + "\nEffective group id: " + ps.getEffectiveGroup()
            + "\nCurrent user: " + ps.getUserInfo(ps.getCurrentUser())
            + "\nEffective user: " + ps.getUserInfo(ps.getEffectiveUser())
            + "\nCurrent group: " + ps.getGroupInfo(ps.getCurrentGroup())
            + "\nEffective group: " + ps.getGroupInfo(ps.getEffectiveGroup())
            + "\nUser 0: " + ps.getUserInfo(0)
            + "\nUser root: " + ps.getUserInfo("root")
            + "\nUser ftp: " + ps.getUserInfo("ftp")
            + "\nGroup 0: " + ps.getGroupInfo(0)
            + "\nGroup root: " + ps.getGroupInfo("root")
            + "\nGroup adm: " + ps.getGroupInfo("adm")
            + "\n");
    }
}
