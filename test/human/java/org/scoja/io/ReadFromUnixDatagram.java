/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003  Mario Mart�nez
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
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

import org.scoja.io.posix.Posix;
import org.scoja.io.posix.PosixFree;
import org.scoja.io.posix.PosixNative;

public class ReadFromUnixDatagram
    extends Thread {

    public static void main(final String[] args)
    throws Exception {
        if (args.length < 3 || 5 < args.length) {
            System.err.print(
                "Usage:"
                + "\n  " + ReadFromUnixDatagram.class.getName()
                +   " <posix provider> <socket file> <threads>"
                +   " [<duration> [<timeout>]]"
                + "\nPosix providers:"
                + "\n    " + PosixFree.class.getName()
                + "\n    " + PosixNative.class.getName()
                + "\nDuration,"
                + "\n  total milliseconds this server is reading;"
                + "\n  if absent or nonpositive, reads forever."
                + "\n");
            System.exit(-1);
        }
        
        int argc = 0;
        final String posixLike = args[argc++];
        final UnixSocketAddress sa = new UnixSocketAddress(args[argc++]);
        final int threads = Integer.parseInt(args[argc++]);
        long duration = -1;
        if (argc < args.length) {
            duration = Long.parseLong(args[argc++]);
        }
        long timeout = -1;
        if (argc < args.length) {
            timeout = Long.parseLong(args[argc++]);
        }
        
        Posix.setPosix(posixLike);
        
        final UnixDatagramSocket socket = new UnixDatagramSocket();
        socket.setReuseAddress(true);
        sa.clear();
        socket.bind(sa);
        if (timeout > 0) socket.setSoTimeout(timeout);
        
        final Thread t[] = new Thread[threads];
        for (int i = 0; i < t.length; i++) {
            t[i] = new ReadFromUnixDatagram(i, socket);
            t[i].start();
        }
        System.out.println(threads + " threads reading from " + socket);
        
        if (duration > 0) {
            new Timer(true).schedule(new TimerTask() {
                    public void run() {
                        try {
                            System.out.print(
                                "Processing time ended: closing socket\n");
                            socket.close();
                            System.out.print("Socket closed\n");
                        } catch (Throwable e) {
                            System.err.print("Error while closing socket:\n");
                            e.printStackTrace(System.err);
                        }
                    }
                }, duration);
        }        
    }
    
    //======================================================================
    protected final int id;
    protected final UnixDatagramSocket socket;
    
    public ReadFromUnixDatagram(final int id,
                                final UnixDatagramSocket socket) {
        this.id = id;
        this.socket = socket;
    }
    
    public void run() {
        final GenericDatagramPacket packet
            = new GenericDatagramPacket(new byte[2*1024]);
        try {
            for (;;) {
                try {
                    socket.receive(packet);
                    System.out.println("Read by " + id + ": " + packet);
                } catch (SocketTimeoutException e) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
