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
package org.scoja.trans.ssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.security.Principal;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus;

import org.scoja.cc.lang.Exceptions;
import org.scoja.cc.io.Buffers;
import org.scoja.trans.Errors;
import org.scoja.trans.ConnectState;
import org.scoja.trans.RemoteInfo;
import org.scoja.trans.TransportLine;
import org.scoja.trans.IStream;
import org.scoja.trans.OStream;
import org.scoja.trans.SelectionHandler;
import org.scoja.trans.InterestInterceptor;
import org.scoja.trans.lc.NoLCLine;

public abstract class SSLLine extends NoLCLine<SSLConf>
    implements InterestInterceptor {

    protected final SSLTransport trans;
    protected final SSLConf.Stacked conf;
    protected final TransportLine base;
    protected ConnectState state;
    protected boolean inputEnded;
    protected SSLEngine ssl;
    protected ByteBuffer netin;
    protected ByteBuffer netout;
    protected ByteBuffer appin;
    protected ByteBuffer fakeappout;
    protected boolean netinPending;
    protected boolean netoutPending;
    protected boolean appinPending;
    
    protected Principal peerPrincipal;
    protected SelectionHandler sh;

    public SSLLine(final SSLTransport trans, final SSLConf conf,
            final TransportLine base, final ConnectState initState) {
        this.trans = trans;
        this.conf = new SSLConf.Stacked(conf);
        this.base = base;
        this.state = initState;
        this.inputEnded = false;
        this.ssl = null;
        this.netout = this.netin = this.appin = this.fakeappout = null;
        this.netinPending = this.netoutPending = this.appinPending = false;
        this.sh = null;
    }
    
    public String layers() { return "ssl-" + base.layers(); }
    
    public SSLConf configuration() { return conf; }
    
    public boolean isBlocking() {
        return base.isBlocking();
    }
    
    public RemoteInfo remote() {
        return new RemoteInfo.Proxy(base.remote()) {
            public Principal principal() { return peerPrincipal; }
        };
    }
    
    public void close()
    throws IOException {
        base.close();
    }

    protected abstract boolean isForClientMode();
        
    public ConnectState connect()
    throws IOException {
        if (state.connectDone()) return state;
        state = ConnectState.CONNECTING;
        final ConnectState baseState = base.connect();
        if (baseState == ConnectState.CONNECTED) {
            ensureSSLEngine();
            performHandshake();
            if (sh != null) sh.updateInterestOps();
        } else if (baseState != ConnectState.CONNECTING) {
            throw Errors.baseConnectionFailed(this);
        }
        return state;
    }
    
    protected void ensureSSLEngine()
    throws IOException {
        if (ssl == null) {
            ssl = trans.createEngine(conf);
            ssl.setUseClientMode(isForClientMode());
            ssl.beginHandshake();
            createInitialBuffers();
        }
    }
    
    protected void createInitialBuffers() {
        final int ps = ssl.getSession().getPacketBufferSize();
        final int as = ssl.getSession().getApplicationBufferSize();
        //System.err.println("Buffer sizes: packet=" + ps + ", app=" + as);
        final int initas = 1024;
        appin = ByteBuffer.allocate(initas); appinPending = false;
        fakeappout = ByteBuffer.allocate(0); fakeappout.flip();
        final int initps = initas + (ps-as);
        netin = ByteBuffer.allocate(initps); netinPending = true;
        netout = ByteBuffer.allocate(initps); netoutPending = false;
    }
    
    protected void performHandshake()
    throws IOException {
        boolean stepDone;
        do {
            if (appinPending) throw new SSLHandshakeException(
                "Illegal handshake state:"
                + " there is peer data before the connection is fulfilled");
            stepDone = perform1(fakeappout, false, false);
            //System.err.println(
            //    "Handshake step performed: " + stepDone + ", " + state);
        } while (stepDone && state == ConnectState.CONNECTING);
        if (state == ConnectState.CONNECTING && inputEnded) {
            throw new IOException("Incomplete handshake");
        }
    }
    
    protected boolean perform1(final ByteBuffer appout,
            final boolean readRequested, final boolean writeRequested)
    throws IOException {
        boolean performed = false;
        //System.err.println("Determining action");
        final SSLEngineAction action
            = determineAction(readRequested, writeRequested);
        //System.err.println("Perform: " + action + ", " + netoutPending);
        if (netoutPending) {
            int send = 0;
            if (action == SSLEngineAction.WRAP) {
                performed = send();
                if (netoutPending) return performed;
            } else if (!base.isBlocking()) {
                performed = send();
            }
        }
        if (action == SSLEngineAction.WRAP) {
            resize:
            for (;;) {
                final SSLEngineResult ar = ssl.wrap(faked(appout), netout);
                //System.err.println("  Tried: " + action
                //        + ", " + ar.getStatus() + ", " + netout.position());
                switch (ar.getStatus()) {
                case OK:
                    netout.flip();
                    netoutPending = true;
                    performed |= send();
                    break resize;
                case BUFFER_OVERFLOW: 
                    shrinkNetout();
                    continue resize;
                case CLOSED:
                    throw new IOException("Closed while wrapping");
                }
            }
        } else if (action == SSLEngineAction.UNWRAP && receive()) {
            resize:
            for (;;) {
                final SSLEngineResult ar = ssl.unwrap(netin, appin);
                //System.err.println("  Tried: " + action
                //        + ", " + ar.getStatus());
                switch (ar.getStatus()) {
                case OK:
                    performed = true;
                    if (appin.position() > 0) {
                        appin.flip();
                        appinPending = true;
                    }
                    break resize;
                case BUFFER_OVERFLOW:
                    shrinkAppin();
                    continue resize;
                case BUFFER_UNDERFLOW:
                    if (netin.remaining() < netin.capacity()) netin.compact();
                    else shrinkNetin();
                    netinPending = true;
                    if (receive()) continue resize;
                    else break resize;
                case CLOSED:
                    throw new IOException("Closed while unwrapping");
                }
            }
        }
        return performed;
    }
    
    protected SSLEngineAction determineAction() {
        SSLEngineAction action = SSLEngineAction.NONE;
        HandshakeStatus hs;
        for (;;) {
            hs = ssl.getHandshakeStatus();
            if (hs != HandshakeStatus.NEED_TASK) break;
            doSSLTasks();
        }
        switch (hs) {
        case FINISHED:
        case NOT_HANDSHAKING:
            if (state == ConnectState.CONNECTING)
                state = ConnectState.CONNECTED;
            try {
                peerPrincipal = ssl.getSession().getPeerPrincipal();
            } catch (javax.net.ssl.SSLPeerUnverifiedException e) {}
            break;
        case NEED_WRAP:
            action = SSLEngineAction.WRAP;
            break;
        case NEED_UNWRAP:
            action = SSLEngineAction.UNWRAP;
            break;
        }
        return action;
    }
    
    protected SSLEngineAction determineAction(
        final boolean readRequested, final boolean writeRequested) {
        SSLEngineAction action = determineAction();
        if (SSLEngineAction.NONE.equals(action)) {
            //When both read and write are requested, do read before 
            // to unstall the peer.
            if (readRequested) action = SSLEngineAction.UNWRAP;
            else if (writeRequested) action = SSLEngineAction.WRAP;
        }
        return action;
    }
    
    protected void doSSLTasks() {
        Runnable task;
        while ((task = ssl.getDelegatedTask()) != null) task.run();
    }    

    protected ByteBuffer faked(final ByteBuffer appout) {
        return (appout != null) ? appout : fakeappout;
    }

    /**
     * @pre !appinPending
     */    
    protected void shrinkAppin()
    throws IOException {
        appin = pureShrink(appin,
                ssl.getSession().getApplicationBufferSize(),
                "Cannot resize after an application input buffer overflow");
    }
    
    /**
     * @pre !netoutPending
     */
    protected void shrinkNetout()
    throws IOException {
        netout = pureShrink(netout,
                ssl.getSession().getPacketBufferSize(),
                "Cannot resize after a network output buffer overflow");
    }
    
    protected void shrinkNetin()
    throws IOException {
        final ByteBuffer tmp = pureShrink(netin,
                ssl.getSession().getPacketBufferSize(),
                "Cannot resize after a network input buffer underflow");
        tmp.put(netin);
        netin = tmp;
    }
    
    protected ByteBuffer pureShrink(final ByteBuffer bb, final int maxSize,
            final String fullSizeError)
    throws IOException {
        final int size = bb.capacity();
        final int newsize = Math.min(2*size, maxSize);
        if (size == newsize) throw new IOException(
            fullSizeError + "(current size " + size + ")");
        return ByteBuffer.allocate(newsize);
    }
        
    /**
     * @return true iff there is pending data to be processed
     *   and there is no evidence of underflow.
     */
    protected boolean receive()
    throws IOException {
        if (netinPending) {
            final int n = base.input().read(netin);
            //System.err.println("Received " + n);
            if (n > 0) {
                netin.flip();
                netinPending = false;
            } else if (n < 0) {
                inputEnded = true;
            }
        }
        return !netinPending;
    }
    
    /**
     * @return true iff same data has been send.
     */
    protected boolean send()
    throws IOException {
        final int n = base.output().write(netout);
        if (!netout.hasRemaining()) {
            netoutPending = false;
            netout.clear();
        }
        return n > 0;
    }
    
    
    public IStream input()
    throws IOException {
        return new SSLIStream();
    }
    
    public OStream output()
    throws IOException {
        return new SSLOStream();
    }
    
    public SelectionHandler register(final SelectionHandler handler) {
        sh = handler;
        handler.addSelectable(this);
        handler.addInterestInterceptor(this);
        base.register(handler);
        return handler;
    }
    
    public int interestOps(final int current) {
        if (state != ConnectState.CONNECTING) return current;
        if (ssl == null) {
            return isForClientMode() ? SelectionKey.OP_WRITE
                : SelectionKey.OP_READ;
        }
        final SSLEngineAction action = determineAction();
        int op = 0;
        if (netinPending || SSLEngineAction.UNWRAP.equals(action))
            op |= SelectionKey.OP_READ;
        if (netoutPending || SSLEngineAction.WRAP.equals(action))
            op |= SelectionKey.OP_WRITE;
        return op;
    }
    
    public String toString() {
        return "SSLLine[base: " + base + "]";
    }

    
    //======================================================================
    protected class SSLIStream extends IStream.Defaults {
    
        public int available()
        throws IOException {
            return appinPending ? appin.remaining() : base.input().available();
        }
        
        public int read(final byte[] bs, final int off, final int len)
        throws IOException {
            ensureInput();
            final int n;
            if (appinPending) {
                n = Buffers.getSome(appin, bs, off, len);
                if (!appin.hasRemaining()) {
                    appin.clear();
                    appinPending = false;
                }
            } else if (inputEnded) {
                n = -1;
            } else {
                n = 0;
            }
            return n;
        }
    
        public int read(final ByteBuffer bs)
        throws IOException {
            ensureInput();
            final int n;
            if (appinPending) {
                n = Buffers.getSome(appin, bs);
                if (!appin.hasRemaining()) {
                    appin.clear();
                    appinPending = false;
                }
            } else if (inputEnded) {
                n = -1;
            } else {
                n = 0;
            }
            return n;
        }
        
        protected void ensureInput()
        throws IOException {
            while (!appinPending && !inputEnded
                    && perform1(fakeappout, true, false));
        }
    }
    
    
    //======================================================================
    protected class SSLOStream extends OStream.Defaults {
        public int write(final ByteBuffer bs)
        throws IOException {
            final int n = bs.remaining();
            while (bs.hasRemaining() && perform1(bs, false, true));
            return n - bs.remaining();
        }
        
        public int flush()
        throws IOException {
            if (netoutPending) {
                while(send());
                if (netoutPending) return netout.remaining();
            }
            return base.output().flush();
        }
    }
    
    
    //======================================================================
    public static class Server extends SSLLine {
    
        protected final SSLService serv;
        
        public Server(final SSLService serv, final TransportLine base) {
            super(serv.trans, serv.conf, base, ConnectState.CONNECTING);
            this.serv = serv;
        }
        
        protected boolean isForClientMode() { return false; }
    }
    
    
    //======================================================================
    public static class Client extends SSLLine {
    
        public final SSLTransport trans;
        
        public Client(final SSLTransport trans) {
            super(trans, trans.conf,
                    trans.base.newClient(), ConnectState.UNCONNECTED);
            this.trans = trans;
        }
        
        protected boolean isForClientMode() { return true; }
    }
}
