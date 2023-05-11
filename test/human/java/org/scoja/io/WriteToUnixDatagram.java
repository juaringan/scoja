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

package org.scoja.io;

import java.io.*;

import org.scoja.io.posix.Posix;
import org.scoja.io.posix.PosixFree;
import org.scoja.io.posix.PosixNative;

public class WriteToUnixDatagram {

    public static void main(final String[] args) throws Exception {
        if (args.length < 2 || 3 < args.length ) {
            System.err.print(
                "Usage:"
                + "\n  " + WriteToUnixDatagram.class.getName()
                +   " <posix like class> <unix socket> [<source>]"
                + "\nPosix like classes:"
                + "\n    " + PosixFree.class.getName()
                + "\n    " + PosixNative.class.getName()
                + "\n");
            System.exit(-1);
        }
        
        int argc = 0;
        final String posixLike = args[argc++];
        final UnixSocketAddress sa = new UnixSocketAddress(args[argc++]);
        BufferedReader reader;
        if (args.length > argc) {
            final String filename = args[argc++];
            reader = new BufferedReader(new FileReader(filename));
        } else {
            reader = new BufferedReader(new InputStreamReader(System.in));
        }
        
        Posix.setPosix(posixLike);
        
        final UnixDatagramSocket socket = new UnixDatagramSocket();
        socket.connect(sa);
        for (;;) {
            final String line = reader.readLine();
            if (line == null) break;
            socket.send(new GenericDatagramPacket(line.getBytes()));
        }
        reader.close();
        socket.close();
    }
}
