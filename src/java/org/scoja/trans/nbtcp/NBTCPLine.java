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
package org.scoja.trans.nbtcp;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.scoja.cc.lang.Exceptions;

import org.scoja.trans.ConnectState;
import org.scoja.trans.RemoteInfo;
import org.scoja.trans.TransportLine;
import org.scoja.trans.IStream;
import org.scoja.trans.OStream;
import org.scoja.trans.SelectionHandler;
import org.scoja.trans.InterestInterceptor;
import org.scoja.trans.tcp.TCPConf;
import org.scoja.trans.tcp.ConfigurableSocket;
import org.scoja.trans.lc.NoLCLine;

/**
 * A SocketChannel state can be explored with
 *   {@link SocketChannel#isOpen},
 *   {@link SocketChannel#isConnectionPending},
 *   and {@link SocketChannel#isConnected}.
 * Tests in Socket are not useful;
 * {@link Socket#isClosed()} is !{@link SocketChannel#isOpen},
 * {@link Socket#isConnected()} is {@link SocketChannel#isConnected},
 * and {@link Socket#isBound()} is also {@link SocketChannel#isConnected}.
 * So a SocketChannel state can be represented with a 3-ary tuple of booleans
 * with the values of isOpen, isConnectionPending and isConnected.
 * Calling only these methods doesn't change the state of a SocketChannel.
 * A call 
 *   to {@link SocketChannel#connect}
 *   or to {@link SocketChannel#finishConnect}
 * is necessary to update the Java view of any native socket change.
 * A SocketChannel brought to life with an accept is in state (T,F,T).
 * A SocketChannel brought to life with a @link ChannelSocket#open()} is in
 * state (T,F,F).
 * The following diagram shows the state changes:
 *   (T,a,b) --[close() or any error]--> (F,a,b)
 *   (T,F,F) --[connect(...)]--> (T,T,F)
 *   (T,T,F) --[finishConnect() without error]--> (T,F,T)
 */
public abstract class NBTCPLine extends NoLCLine<TCPConf>
    implements InterestInterceptor {
    
    protected final ConfigurableSocket conf;
    protected ConnectState state;
    protected SelectionHandler sh;
    
    public NBTCPLine(final TCPConf conf) {
        this.conf = new ConfigurableSocket(new TCPConf.Stacked(conf));
        this.state = ConnectState.UNCONNECTED;
        this.sh = null;
    }
    
    public NBTCPLine(final TCPConf conf, final SocketChannel channel)
    throws IOException {
        this(conf);
        channel.configureBlocking(false);
        this.conf.configure(channel);
        this.conf.socket(channel);
        this.state = ConnectState.CONNECTED;
    }
    
    public String layers() { return "NBTCP"; }
    
    public TCPConf configuration() { return conf; }
    
    public boolean isBlocking() { return false; }
    
    public RemoteInfo remote() {
        return RemoteInfo.Inet.from(conf.socket());
    }
    
    public void close()
    throws IOException {
        final SocketChannel s = conf.socket();
        if (s != null) s.close();
    }
    
    public IStream input()
    throws IOException {
        return new IStream.FromConnectedSocketChannel(conf.socket());
    }
    
    public OStream output()
    throws IOException {
        return new OStream.FromConnectedSocketChannel(conf.socket());
    }
    
    public SelectionHandler register(final SelectionHandler handler) {
        //System.err.println("NBTCP REGISTER: " + conf.socket());
        sh = handler;
        handler.addSelectable(this);
        handler.addInterestInterceptor(this);
        if (conf.socket() != null) sh.register(conf.socket());
        return handler;
    }

    public int interestOps(final int current) {
        return (state == ConnectState.CONNECTING) ? SelectionKey.OP_CONNECT
            : current;
    }
    
    public String toString() {
        return "NBTCPLine[state: " + state + "]";
    }
    
    
    //======================================================================
    public static class Server extends NBTCPLine {
        
        protected final NBTCPService serv;
        
        public Server(final NBTCPService serv, final TCPConf conf,
                final SocketChannel socket)
        throws IOException {
            super(conf, socket);
            this.serv = serv;
        }
        
        public ConnectState connect()
        throws IOException {
            return state;
        }
    }
    
    
    //======================================================================
    public static class Client extends NBTCPLine {
        
        protected final NBTCPTransport trans;
        
        public Client(final NBTCPTransport trans, final TCPConf.Stacked conf) {
            super(conf);
            this.trans = trans;
        }
        
        public ConnectState connect()
        throws IOException {
            if (state.connectDone()) return state;
            SocketChannel s = conf.socket();
            if (s == null) {
                try {
                    s = SocketChannel.open();
                    s.configureBlocking(false);
                    conf.configure(s);
                    if (sh != null) sh.register(s);
                    s.connect(trans.address);
                    state = ConnectState.CONNECTING;
                    conf.socket(s);
                } catch (Throwable e) {
                    if (s != null) s.close();
                    throw Exceptions.uncheckedOr(IOException.class, e);
                }
            }
            //System.err.println(s.isOpen() + " " + s.isConnected()
            //        + " " + s.isConnectionPending());
            if (!s.isOpen()) state = ConnectState.DISCONNECTED;
            else if (s.isConnected()
                || (s.isConnectionPending() && s.finishConnect())) {
                state = ConnectState.CONNECTED;
            }
            if (sh != null) sh.updateInterestOps();
            return state;
        }
    }
}
