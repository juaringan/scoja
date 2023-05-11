/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003  Mario Martínez
 * Copyright (C) 2012  LogTrust
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

import java.io.IOException;
import java.net.*;
import java.util.List;

//import org.scoja.common.PriorityUtils;
import org.scoja.trans.RemoteInfo;
import org.scoja.server.core.ScojaThread;
import org.scoja.server.core.ClusterSkeleton;
import org.scoja.server.core.DecoratedLink;
import org.scoja.server.core.Event;
import org.scoja.server.core.EventContext;
import org.scoja.server.core.Link;
import org.scoja.server.core.Linkable;
import org.scoja.server.core.Measurable;
import org.scoja.server.core.Measure;
import org.scoja.server.core.Serie;
//import org.scoja.server.parser.ParsedEvent;
import org.scoja.server.parser.ProtocolFactory;
import org.scoja.server.parser.LightProtocolFactory;
import org.scoja.server.parser.PacketProtocol;
import org.scoja.server.source.Internal;

public class UDPSource 
    extends ClusterSkeleton
    implements Measurable, DecoratedLink, Runnable {
    
    public static final int DEFAULT_MAX_PACKET_SIZE = 2000;
    public static final int DEFAULT_RECEIVE_BUFFER_SIZE = 0;
    public static final int DEFAULT_SEND_BUFFER_SIZE = 0;
    
    protected final Link link;
    
    protected String ip;
    protected int port;
    protected int rcvsize;
    protected int maxPacketSize;
    protected boolean reuseAddress;
    
    protected final Object ensureLock;
    protected DatagramSocket socket;
    protected ProtocolFactory protocoler;

    protected final Object statsLock;
    protected long partialPackets, totalPackets;
    protected long partialBytes, totalBytes;
    
    public UDPSource() {
        this.link = new Link();
        this.ip = "127.0.0.1";
        this.port = 514;
        this.rcvsize = DEFAULT_RECEIVE_BUFFER_SIZE;
        this.maxPacketSize = DEFAULT_MAX_PACKET_SIZE;
        this.reuseAddress = false;
        this.ensureLock = new Object();
        this.socket = null;
        this.protocoler = LightProtocolFactory.getInstance();
        
        this.statsLock = new Object();
        this.partialPackets = this.totalPackets = 0;
        this.partialBytes = this.totalBytes = 0;
    }
    
    public Linkable getLinkable() {
        return link;
    }
    
    public void setIp(final String ip) {
        this.ip = ip;
    }
    
    public void setPort(final int port) {
        this.port = port;
    }
    
    public void setReceiveBuffer(final int rcvsize)
    throws SocketException {
        this.rcvsize = rcvsize;
        if (socket != null) socket.setReceiveBufferSize(rcvsize);
    }
    
    public void setMaxPacketSize(final int max) {
        this.maxPacketSize = max;
    }
    
    public void setReuseAddress(final boolean reuse) {
        this.reuseAddress = reuse;
    }
    
    public void setProtocol(final ProtocolFactory protocoler) {
        this.protocoler = protocoler;
    }
    
    public void start() {
        Internal.warning(Internal.SOURCE_UDP, "Starting " + this);
        super.start();
        super.startAllThreads();
    }
    
    public void shouldStop() {
        super.shouldStop();
        if (socket != null) {
            socket.close();
        }
    }
    
    public void run() {
        ensureSocket();
        processPackets();
        threadStopped();
    }
    
    protected void ensureSocket() {
        synchronized (ensureLock) {
            if (socket != null) return;
            
            final Serie delays = new Serie.Bounded(60, new Serie.Exp(1, 2));
            while (!stopRequested()) {
                Throwable error;
                try {
                    Internal.warning(Internal.SOURCE_UDP,
                                     "Opening server socket for " + this);
                    openSocket();
                    break;
                } catch (Throwable e) {
                    error = e;
                }
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                final double delay = delays.next();
                Internal.err(Internal.SOURCE_UDP, 
                             "Source " + this + " cannot listen!"
                             + " I will retry after " + delay + " seconds.",
                             error);
                try {
                    ScojaThread.sleep(delay);
                } catch (InterruptedException e) {}
            }
        }
    }
    
    protected void openSocket()
    throws SocketException, UnknownHostException {
        if (reuseAddress) {
            /* It seems there is an error in DatragramSocket bind
             * implementation. An error "address already in use" is
             * always raised when this execution path is followed. */
            socket = new DatagramSocket();
            socket.disconnect();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(ip,port));
        } else {
            socket = new DatagramSocket(port, InetAddress.getByName(ip));
        }
        if (rcvsize > 0) socket.setReceiveBufferSize(rcvsize);
    }
    
    protected void processPackets() {
        final Thread thread = Thread.currentThread();
        if (!(thread instanceof ScojaThread)) {
            Internal.emerg(Internal.SOURCE_UDP,
                           "Refusing to be executed by thread " + thread 
                           + ": it isn't a ScojaThread.");
            return;
        }
        final ScojaThread sthread = (ScojaThread)thread;
        
        final PacketProtocol proto = protocoler.newPacketProtocol(link);
        final byte[] data = new byte[maxPacketSize];
        final DatagramPacket packet = new DatagramPacket(data, data.length);
        while (!stopRequested()) {
            try {
                socket.receive(packet);
            } catch (Throwable e) {
                Internal.err(
                    Internal.SOURCE_UDP, "While [" + toString()
                    + "] was receiving ", e);
            }
            final int len = packet.getLength();
            if (len > 0) try {
                measurePacket(len);
                proto.processPacket(
                    new RemoteInfo.Inet(packet.getAddress()),
                    data, 0, packet.getLength());
                /*
                final Event event = new ParsedEvent(
                    new RemoteInfo.Inet(packet.getAddress()),
                    data, 0, packet.getLength());
                final EventContext ectx = new EventContext(event);
                sthread.setEventContext(ectx);
                if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
                    Internal.debug(ectx, Internal.SOURCE_UDP,
                                   "Received: " + ectx);
                }
                link.process(ectx);
                */
            } catch (Throwable e) {
                Internal.err(
                    Internal.SOURCE_UDP,
                    "While [" + toString() + "] was processing packet from "
                    + packet.getAddress(), e);
            }
        }
    }
    
    protected void measurePacket(final int len) {
        synchronized (statsLock) {
            partialPackets++;
            partialBytes += len;
        }
    }
    
    public Measure.Key getMeasureKey() {
        return new Measure.Key("source", "udp", ip + ":" + port); 
    }
    
    public void stats(final List<Measure> measures) {
        final long pp, tp, pb, tb;
        synchronized (statsLock) {
            tp = totalPackets += partialPackets;
            tb = totalBytes += partialBytes;
            pp = partialPackets; partialPackets = 0;
            pb = partialBytes; partialBytes = 0;
        }
        final Measure.Key key = getMeasureKey();
        measures.add(new Measure(key, "packets", pp, tp));
        measures.add(new Measure(key, "bytes", pb, tb));
        super.stats(key, measures);
    }
    
    public String toString() {
        return "UDP source listening at " + ip + ":" + port;
    }
    
    public String getName4Thread() {
        return "dgram@" + ip + ":" + port;
    }
}
