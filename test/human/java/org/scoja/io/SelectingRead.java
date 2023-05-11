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

import java.io.InputStream;
import java.net.DatagramPacket;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.scoja.io.InetSocketAddress;
import org.scoja.io.posix.Posix;
import org.scoja.io.posix.PosixFree;
import org.scoja.io.posix.PosixNative;

/**
 * This is the main 
 */
public class SelectingRead {

    static boolean verbose = false;
    
    static void debug(final String message) {
        if (verbose) System.out.println(message);
    }

    static void help(final String message) {
        if (message != null) System.err.println("Error: " + message);
        System.err.print(
            "Usage:" 
            + "\n  java " + SelectingRead.class.getName() 
            + " [-v]"
            + " [-k <duration in millis>]"
            + " [-t <timeout in millis>]"
            + " [-p <posix provider>]"
            + " {-ud <unix domain datagram socket>"
            + " {-us <unix domain stream socket>}"
            + " {-id ip port}"
            + " {-is ip port}"
            + "\nPosix providers:"
            + "\n    " + PosixFree.class.getName()
            + "\n    " + PosixNative.class.getName()
            + "\n");
        System.exit(-1);
    }
        
    public static void main(final String[] args) throws Exception {
        if (args.length == 0) help(null);

        long towait = -1;
        long timeout = 0;
        Selector all = null;
        int argc = 0, remain = args.length;
        while (remain > 0) {
            final String opt = args[argc]; argc++; remain--;
            if ("-v".equals(opt)) {
                verbose = true;
            } else if ("-k".equals(opt) && remain > 0) {
                towait = Long.parseLong(args[argc]);
                argc++; remain--;
            } else if ("-t".equals(opt) && remain > 0) {
                timeout = Long.parseLong(args[argc]);
                argc++; remain--;
            } else if ("-p".equals(opt) && remain > 0) {
                Posix.setPosix(args[argc]);
                argc++; remain--;
            } else if ("-ud".equals(opt) && remain > 0) {
                if (all == null) all = new Selector();
                final UnixSocketAddress sa
                    = new UnixSocketAddress(args[argc]);
                argc++; remain--;
                final UnixDatagramSocket socket = new UnixDatagramSocket();
                sa.clear();
                socket.bind(sa);
                socket.register(all, SelectionKey.OP_READ,
                                new UnixDatagramManager());
            } else if ("-us".equals(opt) && remain > 0) {
                if (all == null) all = new Selector();
                final UnixSocketAddress sa
                    = new UnixSocketAddress(args[argc]);
                argc++; remain--;
                final UnixServerSocket socket = new UnixServerSocket();
                sa.clear();
                socket.setReuseAddress(true);
                socket.bind(sa);
                socket.register(all, SelectionKey.OP_READ,
                                new UnixServerManager());
            } else if ("-id".equals(opt) && remain >= 2) {
                if (all == null) all = new Selector();
                final String ip = args[argc++];
                final int port = Integer.parseInt(args[argc++]);
                remain -= 2;
                final InetSocketAddress sa = new InetSocketAddress(ip, port);
                final InetDatagramSocket socket = new InetDatagramSocket();
                socket.bind(sa);
                socket.register(all, SelectionKey.OP_READ,
                                new InetDatagramManager());
            } else if ("-is".equals(opt) && remain > 0) {
                if (all == null) all = new Selector();
                final String ip = args[argc++];
                final int port = Integer.parseInt(args[argc++]);
                remain -= 2;
                final InetSocketAddress sa = new InetSocketAddress(ip, port);
                final InetServerSocket socket = new InetServerSocket();
                socket.setReuseAddress(true);
                socket.bind(sa);
                socket.register(all, SelectionKey.OP_READ,
                                new InetServerManager());
            } else {
                help("Unknown option \"" +opt+ "\" or argument expected");
            }
        }
        
        if (towait > 0) {
            final Selector finalAll = all;
            new Timer(true).schedule(new TimerTask() {
                    public void run() {
                        try {
                            System.err.print(
                                "Processing time ended: closing sockets\n");
                            finalAll.close();
                            System.err.print("All sockets closed\n");
                        } catch (Throwable e) {
                            System.err.print("Error while closing sockets:\n");
                            e.printStackTrace(System.err);
                        }
                    }
                }, towait);
        }

        int n = 0;
        for (;;) {
            debug(n + ". selecting at\n  " + all);
            all.select(timeout);
            final Set selected = all.selectedKeys();
            debug(n + ". selected\n  " + selected);
            final Iterator it = selected.iterator();
            while (it.hasNext()) {
                final SelectionKey key = (SelectionKey)it.next();
                final Manager manager = (Manager)key.attachment();
                manager.manage(key);
                it.remove();
            }
            n++;
        }
    }
    
    static interface Manager {
        public void manage(SelectionKey key) throws Exception;
    }
    
    static class UnixDatagramManager implements Manager {
        public void manage(final SelectionKey key) throws Exception {
            final GenericDatagramPacket packet
                = new GenericDatagramPacket(1024);
            final UnixDatagramSocket socket
                = (UnixDatagramSocket)key.channel();
            debug("Receiving from " + socket);
            socket.receive(packet);
            if (verbose) {
                debug("Received from " + socket + "\n  " + packet);
            } else {
                System.out.write
                    (packet.getData(), packet.getOffset(), packet.getLength());
                System.out.println();
            }
        }
    }
    
    static class InetDatagramManager implements Manager {
        public void manage(final SelectionKey key) throws Exception {
            final DatagramPacket packet
                = new DatagramPacket(new byte[1024], 1024);
            final InetDatagramSocket socket
                = (InetDatagramSocket)key.channel();
            debug("Receiving from " + socket);
            socket.receive(packet);
            if (verbose) {
                debug("Received from " + socket + "\n  " + packet);
            } else {
                System.out.write
                    (packet.getData(), packet.getOffset(), packet.getLength());
                System.out.println();
            }
        }
    }
    
    static class UnixServerManager implements Manager {
        public void manage(final SelectionKey key) throws Exception {
            final UnixServerSocket socket = (UnixServerSocket)key.channel();
            debug("Accepting at " + socket);
            final UnixSocket child = socket.accept();
            debug("Accepted at " + socket + ": " + child);
            child.register(key.selector(), SelectionKey.OP_READ,
                           new UnixStreamManager());
        }
    }
    
    static class InetServerManager implements Manager {
        public void manage(final SelectionKey key) throws Exception {
            final InetServerSocket socket = (InetServerSocket)key.channel();
            debug("Accepting at " + socket);
            final InetSocket child = socket.accept();
            debug("Accepted at " + socket + ": " + child);
            child.register(key.selector(), SelectionKey.OP_READ,
                           new InetStreamManager());
        }
    }
    
    static class UnixStreamManager implements Manager {
        public void manage(final SelectionKey key) throws Exception {
            final UnixSocket socket = (UnixSocket)key.channel();
            debug("Reading at " + socket);
            final InputStream is = socket.getInputStream();
            final byte[] buffer = new byte[1024];
            final int readed = is.read(buffer);
            if (readed == -1) {
                debug("Ended: " + socket);
                key.cancel();
                socket.close();
            } else {
                if (verbose) {
                    debug("Readed " + readed + " bytes at " + socket
                          + "\n  " + new String(buffer,0,readed));
                } else {
                    System.out.write(buffer, 0, readed);
                }
            }
        }
    }
    
    static class InetStreamManager implements Manager {
        public void manage(final SelectionKey key) throws Exception {
            final InetSocket socket = (InetSocket)key.channel();
            debug("Reading at " + socket);
            final InputStream is = socket.getInputStream();
            final byte[] buffer = new byte[1024];
            final int readed = is.read(buffer);
            if (readed == -1) {
                debug("Ended: " + socket);
                key.cancel();
                socket.close();
            } else {
                if (verbose) {
                    debug("Readed " + readed + " bytes at " + socket
                          + "\n  " + new String(buffer,0,readed));
                } else {
                    System.out.write(buffer, 0, readed);
                }
            }
        }
    }
}
