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
 * This test shows information for some standard users and groups.
 * It shows the same information as {@link PosixSystemTest},
 * but gets it from {@link org.scoja.io.posix.UserInfo}
 * and {@link org.scoja.io.posix.GroupInfo} classes instead of system
 * {@link org.scoja.io.posix.PosixLike}.
 */
public class UserGroupInfoTest {

    public static void main(final String[] args) throws Throwable {
        if (args.length != 1) {
            System.err.print(
                "java " + UserGroupInfoTest.class.getName()
                + " <posix like class>"
                + "\nPosix like classes:"
                + "\n    " + PosixFree.class.getName()
                + "\n    " + PosixNative.class.getName()
                + "\n");
            System.exit(-1);
        }            
        
        int argc = 0;
        final String posixLike = args[argc++];
        
        Posix.setPosix(posixLike);
        
        System.out.print(
            "Current user id: " + UserInfo.getCurrent().getID()
            + "\nEffective user id: " + UserInfo.getEffective().getID()
            + "\nCurrent group id: " + GroupInfo.getCurrent().getID()
            + "\nEffective group id: " + GroupInfo.getEffective().getID()
            + "\nCurrent user: " + UserInfo.getCurrent()
            + "\nEffective user: " + UserInfo.getEffective()
            + "\nCurrent group: " + GroupInfo.getCurrent()
            + "\nEffective group: " + GroupInfo.getEffective()
            + "\nUser 0: " + UserInfo.forID(0)
            + "\nUser root: " + UserInfo.forName("root")
            + "\nUser ftp: " + UserInfo.forName("ftp")
            + "\nGroup 0: " + GroupInfo.forID(0)
            + "\nGroup root: " + GroupInfo.forName("root")
            + "\nGroup adm: " + GroupInfo.forName("adm")
            + "\n");
    }
}
