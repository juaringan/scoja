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
 * {@link org.scoja.io.posix.PosixFilesystem}
 * of the {@link org.scoja.io.posix.PosixLike} class parameter.
 */
public class PosixFilesystemTest {

    public static void main(final String[] args) throws Throwable {
        if (args.length != 1) {
            System.err.println(
                "java " + PosixFilesystemTest.class.getName()
                + " PosixLike"
                + "\nPosixLike:"
                + "\n    " + PosixFree.class.getName()
                + "\n    " + PosixNative.class.getName());
            System.exit(-1);
        }            
        
        int argc = 0;
        final String posixLike = args[argc++];
        
        Posix.setPosix(posixLike);
        final PosixFilesystem ps = Posix.getPosix();
        
        final File tmp1 = File.createTempFile("posixtest", ".tmp");
        System.out.println(tmp1);
        ps.setFileOwner(tmp1.toString(), PosixLike.KEEP_USER, 100);
        ps.setFileMode(tmp1.toString(), 0600);
        System.out.println(ps.getFileStat(tmp1.toString()));
        
        final File tmp2 = File.createTempFile("posixtest", ".tmp");
        System.out.println(tmp2);
        final FileOutputStream out = new FileOutputStream(tmp2);
        ps.setFileOwner(out.getFD(), PosixLike.KEEP_USER, 100);
        ps.setFileMode(out.getFD(), 0640);
        System.out.println(ps.getFileStat(out.getFD()));
        out.close();
    }
}
