/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2010  Mario Martínez
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
package org.scoja.speed;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import org.scoja.cc.io.Charsets;
import org.scoja.cc.net.XAddress;

public class Repeater {

    public static void main(final String[] args)
    throws IOException {
        if (args.length != 3) {
            System.err.println(
                "java " + Repeater.class.getName()
                + " <server-address> <count> <message>");
            System.exit(1);
        }
        int i = 0;
        final InetSocketAddress addr = XAddress.hostPort(args[i++], 1514);
        final int times = Integer.parseInt(args[i++]);
        final byte[] message = Charsets.latin1(args[i++]);
        
        final Socket socket = new Socket(addr.getAddress(), addr.getPort());
        socket.setTcpNoDelay(true);
        final OutputStream out = socket.getOutputStream();
        for (i = 0; i < times; i++) {
            out.write(message);
        }
        out.flush();
        socket.close();
    }
}
