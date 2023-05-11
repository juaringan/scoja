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

/**
 * This is a trivial {@link WriteToUnixStream} variant.
 * It sends extra arguments instead of reading data from a file.
 * We pretended to investigate how consecutive sends are joined.
 */
public class SendToUnixStream {

    public static void main(final String[] args) throws Exception {
        if (args.length < 2) {
            System.err.print(
                "Usage:"
                + "\n  " + WriteToUnixStream.class.getName()
                +   " <posix like class> <unix socket> {<packets>}"
                + "\nPosix like classes:"
                + "\n    " + PosixFree.class.getName()
                + "\n    " + PosixNative.class.getName()
                + "\n");
            System.exit(-1);
        }
        
        int argc = 0;
        final String posixLike = args[argc++];
        final UnixSocketAddress sa = new UnixSocketAddress(args[argc++]);
        
        Posix.setPosix(posixLike);
        
        final UnixSocket socket = new UnixSocket();
        socket.connect(sa);
        final OutputStream target = socket.getOutputStream();
        while (argc < args.length) {
            target.write(args[argc].getBytes());
            argc++;
        }
        socket.close();
    }
}
