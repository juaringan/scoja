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
import java.util.Timer;
import java.util.TimerTask;

import org.scoja.io.posix.Posix;
import org.scoja.io.posix.PosixFree;
import org.scoja.io.posix.PosixNative;

public class SelectingPipeTest {

    public static void main(final String[] args)
    throws Exception {
        if (args.length < 1 || 2 < args.length) {
            System.err.print(
                "Usage:" 
                + "\n  java " + SelectingPipeTest.class.getName() 
                + " <posix provider> [<duration in millis>]"
                + "\nPosix providers:"
                + "\n    " + PosixFree.class.getName()
                + "\n    " + PosixNative.class.getName()
                + "\n");
            System.exit(-1);
        }

        int argc = 0;
        final String posix = args[argc++];
        int towait = -1;
        if (argc < args.length) towait = Integer.parseInt(args[argc++]);
        
        Posix.setPosix(posix);
        final Pipe pp = new Pipe();
        
        final Thread writer = new Thread() {
                public void run() {
                    try {
                        final OutputStream os = pp.getSink();
                        final byte[] buffer = new byte[10];
                        int i = 0;
                        for (;;) {
                            for (int j = 0; j < buffer.length; j++) {
                                buffer[j] = (byte)('a' + i);
                                i = (i+1) % ('z'-'a'+1);
                            }
                            os.write(buffer);
                        }
                    } catch (IOException e) {
                        System.err.println("Error at writer:");
                        e.printStackTrace(System.err);
                    }
                }
            };
        
        final Thread reader = new Thread() {
                public void run() {
                    int total = 0;
                    try {
                        final PipeInputStream is = pp.getSource();
                        final Selector selector = new Selector();
                        final SelectionKey sk
                            = is.register(selector, SelectionKey.OP_READ);
                        final byte[] buffer = new byte[99];
                        for (;;) {
                            final int n = selector.select();
                            System.out.print("\nSelected: " + n);
                            final int read = is.read(buffer);
                            if (read == -1) break;
                            total += read;
                            System.out.print("\n" + total + ": ");
                            System.out.write(buffer, 0, read);
                        }
                    } catch (IOException e) {
                        System.err.println("Error at reader:");
                        e.printStackTrace(System.err);
                    }
                }
            };
        
        if (towait > 0) {
            new Timer().schedule(new TimerTask() {
                    public void run() {
                        try {
                            System.err.print(
                                "Processing time ended: closing pipe\n");
                            pp.closeSink();
                            System.err.print("Pipe closed\n");
                        } catch (Throwable e) {
                            System.err.print("Error while closing pipe:\n");
                            e.printStackTrace(System.err);
                        }
                    }
                }, towait);
        }
        
        writer.start();
        reader.start();
    }
}
