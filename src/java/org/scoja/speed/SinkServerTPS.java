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
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;

/**
 * A Sink Server with a Thread per Socket.
 * Bounds to a given ip:port and listen everything counting how many bytes are
 * received.
 */
public class SinkServerTPS {

    public static void main(final String[] args) {
        final SinkServerTPS server = new SinkServerTPS();
        server.processArguments(args);
        server.doIt();
    }
    
    protected String ip = "0.0.0.0";
    protected int port = 1514;
    
    protected Speedometer speed = null;
    protected Acceptor server = null;
    protected Thread serverRunner = null;
    
    public void processArguments(final String[] args) {
    }
    
    public void doIt() {
        speed = new Speedometer(new int[] {1000, 10*1000});
        try {
            server = new Acceptor();
            serverRunner = new Thread(server);
            serverRunner.start();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        for (;;) {
            System.out.println(speed);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
        }
    }
    
    protected class Acceptor implements Runnable {
        public void run() {
            ServerSocket socket = null;
            for (;;) {
                try {
                    socket = new ServerSocket();
                    socket.bind(new InetSocketAddress(ip, port));
                    for (;;) {
                        final Socket s = socket.accept();
                        System.err.println("Connection accepted from "
                                + s.getRemoteSocketAddress());
                        new Thread(new Reader(s)).start();
                    }
                } catch (Throwable e) {
                    System.err.println("Error while accepting connections: "
                            + e.getMessage());
                } finally {
                    if (socket != null) 
                        try {socket.close();} catch (Throwable ignored) {}
                }
            }
        }
    }
    
    protected class Reader implements Runnable {
        protected final Socket socket;
        protected final InputStream in;
        
        public Reader(final Socket socket)
        throws IOException {
            this.socket = socket;
            this.in = socket.getInputStream();
        }
        
        public void run() {
            final SocketAddress remote = socket.getRemoteSocketAddress();
            System.err.println("Starting to read from " + remote);
            final byte[] buffer = new byte[16*1024];
            try {
                for (;;) {
                    final int n = in.read(buffer);
                    if (n < 0) break;
                    //System.err.println(n);
                    speed.consider(n);
                }
            } catch (Throwable e) {
                System.err.println("Error while reading from " + remote + ": "
                        + e.getMessage());
            }
            System.err.println("Ended reading from " + remote);
        }
    }
}
