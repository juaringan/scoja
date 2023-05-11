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

package org.scoja.server.source;

import org.scoja.server.core.ScojaThread;
import org.scoja.server.core.ClusterSkeleton;
import org.scoja.server.core.DecoratedLink;
import org.scoja.server.core.Linkable;
import org.scoja.server.core.Link;
import org.scoja.server.core.Event;
import org.scoja.server.core.EventContext;
import org.scoja.server.core.Serie;
import org.scoja.server.parser.ParsedEvent;

import org.scoja.io.GenericDatagramPacket;
import org.scoja.io.UnixDatagramSocket;
import org.scoja.io.UnixSocketAddress;
import org.scoja.io.posix.FileAttributes;
import org.scoja.io.posix.PosixFile;
import org.scoja.trans.RemoteInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.io.IOException;

public class UnixDatagramSource 
    extends ClusterSkeleton
    implements DecoratedLink, Runnable {

    public static final int DEFAULT_MAX_PACKET_SIZE = 2000;
    
    protected final Link link;
    protected final RemoteInfo peer;

    protected PosixFile filename;
    protected int maxPacketSize;
    protected boolean reuseAddress;

    protected UnixDatagramSocket socket;

    public UnixDatagramSource()
    throws UnknownHostException, IOException {
        this.link = new Link();
	this.peer = new RemoteInfo.Inet(InetAddress.getLocalHost());
        this.filename = new PosixFile(
            "/dev/log", new FileAttributes("root", "root", "rw-rw-rw-"));
        this.maxPacketSize = DEFAULT_MAX_PACKET_SIZE;
        this.reuseAddress = false;
        this.socket = null;
    }

    public Linkable getLinkable() {
        return link;
    }
    
    public void setFile(final PosixFile filename) {
        this.filename = filename;
    }
    
    public void setMaxPacketSize(final int max) {
        this.maxPacketSize = max;
    }
    
    public void setReuseAddress(final boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }
    
    
    public void start() {
        Internal.warning(Internal.SOURCE_UNIX_DGRAM, "Starting " + this);
        super.start();
        super.startAllThreads();
    }

    public void shouldStop() {
        super.shouldStop();
        closeSocket();
    }

    public void run() {
        ensureSocket();
        processPackets();
        threadStopped();
    }
    
    protected synchronized void ensureSocket() {
        if (socket != null) return;
        
        final Serie delays = new Serie.Bounded(60, new Serie.Exp(1, 2));
        while (!stopRequested()) {
            Throwable error;
            try {
                Internal.warning(Internal.SOURCE_UNIX_DGRAM,
                                 "Opening server socket for " + this);
                openSocket();
                break;
            } catch (Throwable e) {
                error = e;
            }
            closeSocket();
            final double delay = delays.next();
            Internal.err(Internal.SOURCE_UNIX_DGRAM, 
                         "Source " + this + " cannot listen!"
                         + " I will retry after " + delay + " seconds.",
                         error);
            try {
                ScojaThread.sleep(delay);
            } catch (InterruptedException e) {}
        }
    }
    
    protected void openSocket()
    throws IOException {
        socket = new UnixDatagramSocket();
        final UnixSocketAddress unixAddr = new UnixSocketAddress(filename);
        socket.setReuseAddress(reuseAddress);
        unixAddr.clear();
        socket.bind(unixAddr);
    }
    
    protected void closeSocket() {
        if (socket != null) {
            try { socket.close(); } catch (SocketException e) {}
            socket = null;
        }
    }
    
    protected void processPackets() {
        final Thread thread = Thread.currentThread();
        if (!(thread instanceof ScojaThread)) {
            Internal.emerg(Internal.SOURCE_UNIX_DGRAM,
                           "Refusing to be executed by thread " + thread 
                           + " that isn't a ScojaThread.");
            return;
        }
        
        final ScojaThread sthread = (ScojaThread)thread;
	final byte[] data = new byte[maxPacketSize];
        final GenericDatagramPacket packet = new GenericDatagramPacket(data);
        while (!stopRequested()) {
            try {
		socket.receive(packet);
                final Event event
                    = new ParsedEvent(peer, data, 0, packet.getLength());
                final EventContext ectx = new EventContext(event);
                sthread.setEventContext(ectx);
                link.process(ectx);
            } catch (Throwable e) {
                Internal.err(Internal.SOURCE_UNIX_DGRAM,
                             "While receiving from " + socket, e);
            }
        }
    }

    public String toString() {
        return "UNIX udp source listening at " + filename;
    }
    
    public String getName4Thread() {
        return "dgram@" + filename;
    }
}
