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
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

import org.scoja.io.posix.Posix;
import org.scoja.io.posix.PosixFree;
import org.scoja.io.posix.PosixNative;

public class ReadFromUnixStream
    extends Thread {
    
    static long timeout;

    public static void main(final String[] args)
    throws Exception {
        if (args.length < 3 || 5 < args.length) {
            System.err.print(
                "Usage:"
                + "\n  " + ReadFromUnixDatagram.class.getName()
                +   " <posix provider> <socket file> <threads>"
                +   " [<duration> [<timeout>]]"
                + "\nPosix provider:"
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
        timeout = -1;
        if (argc < args.length) {
            timeout = Long.parseLong(args[argc++]);
        }
        
        Posix.setPosix(posixLike);
        
        final UnixServerSocket ss = new UnixServerSocket();
        ss.setReuseAddress(true);
        sa.clear();
        ss.bind(sa);
        if (timeout > 0) ss.setSoTimeout(timeout);
        
        final Thread t[] = new Thread[threads];
        for (int i = 0; i < t.length; i++) {
            t[i] = new ReadFromUnixStream(i, ss);
            t[i].start();
        }
        System.err.println(threads + " threads reading from " + ss);
        
        if (duration > 0) {
            new Timer(true).schedule(new TimerTask() {
                    public void run() {
                        try {
                            System.err.print(
                                "Processing time ended: closing socket\n");
                            ss.close();
                            System.err.print("Socket closed\n");
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
    protected final UnixServerSocket ss;
    
    public ReadFromUnixStream(final int id, final UnixServerSocket ss) {
        this.id = id;
        this.ss = ss;
    }
    
    public void run() {
        try {
            for (;;) {
                try {
                    System.err.println(id + " is ready to accept");
                    final UnixSocket socket = ss.accept();
                    System.err.println(id + " accepted: " + socket);
                    read(socket);
                } catch (SocketTimeoutException e) {
                    //System.err.println("Accept timeout");
                }
            }
        } catch (Exception e) {
            System.err.println("Accept exception:");
            e.printStackTrace(System.err);
        }
    }
    
    public void read(final UnixSocket socket) {
        final byte[] buffer = new byte[1024];
        try {
            try {
                if (timeout > 0) socket.setSoTimeout(timeout);
                final InputStream is = socket.getInputStream();
                for (;;) {
                    int readed = 0;
                    try {
                        readed = is.read(buffer);
                    } catch (SocketTimeoutException e) {
                        //System.err.println("Read timeout");
                    }
                    //System.err.println("Readed: " + readed);
                    if (readed == -1) break;
                    System.out.write(buffer, 0, readed);
                }
            } finally {
                socket.close();
            }
        } catch (Exception e) {
            System.err.println("Read exception:");
            e.printStackTrace(System.err);
        }
    }
}
