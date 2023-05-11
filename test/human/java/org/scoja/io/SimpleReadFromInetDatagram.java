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
import java.net.*;

public class SimpleReadFromInetDatagram extends Thread {

    public static void main(final String[] args) throws Exception {
        System.out.println("SimpleReadFromInetDatagram.main");
        int argc = 0;
        final String host = args[argc++];
        final int port = Integer.parseInt(args[argc++]);
        final InetSocketAddress address
            = new InetSocketAddress(InetAddress.getByName(host),port);
        final InetDatagramSocket socket = new InetDatagramSocket(address);
        final DatagramPacket packet
            = new DatagramPacket(new byte[2*1024], 2*1024);
        new Thread() {
                public void run() {
                    try {
                        for (;;) {
                            socket.receive(packet);
                            System.out.println(packet);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                    }
                }
            }.start();
        try {
            Thread.sleep(2*1000);
            System.out.println("A cerrar");
            socket.close();
            System.out.println("Cerrado");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
