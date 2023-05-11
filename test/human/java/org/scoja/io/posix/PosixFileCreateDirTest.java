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
 * This test creates a directory with the given user/group/perm details.
 * It uses {@link org.scoja.io.posix.PosixFile}
 * and {@link org.scoja.io.posix.FileAttributes}.
 */
public class PosixFileCreateDirTest {

    public static void main(final String[] args) throws Throwable {
        if (args.length != 5) {
            System.err.println(
                "java " + PosixFileCreateDirTest.class.getName()
                + " PosixLike filename user group operm"
                + "\nPosixLike:"
                + "\n    " + PosixFree.class.getName()
                + "\n    " + PosixNative.class.getName());
            System.exit(-1);
        }
        
        int argc = 0;
        final String posixLike = args[argc++];
        final String filename = args[argc++];
        final String user = args[argc++];
        final String group = args[argc++];
        final int perm = Integer.parseInt(args[argc++],8);
        
        Posix.setPosix(posixLike);
        new PosixFile(filename, new FileAttributes(user, group, perm))
            .makeDirectories();
    }
}
