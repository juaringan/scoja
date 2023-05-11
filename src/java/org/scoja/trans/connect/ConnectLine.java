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
package org.scoja.trans.connect;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;

import org.scoja.cc.lang.Exceptions;
import org.scoja.cc.io.Charsets;
import org.scoja.cc.minihttp.HttpListener;
import org.scoja.cc.minihttp.HttpRequestPreface;
import org.scoja.cc.minihttp.HttpResponsePreface;
import org.scoja.cc.minihttp.HttpResponsePrefaceParser;
import org.scoja.trans.*;
import org.scoja.trans.lc.NoLCLine;

public class ConnectLine extends NoLCLine<ConnectConf>
    implements InterestInterceptor {

    protected final ConnectTransport trans;
    protected final ConnectConf.Stacked conf;
    protected final TransportLine base;
    
    protected SelectionHandler sh;
    protected ConnectState state;
    protected int round;
    
    protected HttpRequestPreface req;
    protected byte[] out;
    protected int outNext;
    
    protected byte[] in;
    protected int inNext;
    protected int inEnd;
    protected HttpResponsePrefaceParser respParser;
    protected HttpResponsePreface resp;
    
    
    protected ConnectLine(final ConnectTransport trans,
            final ConnectConf.Stacked conf) {
        this.trans = trans;
        this.conf = new ConnectConf.Stacked(conf);
        this.base = trans.base.newClient();
        
        this.sh = null;
        this.state = ConnectState.UNCONNECTED;
        this.round = 0;
        
        this.req = null;
        this.out = null;
        this.outNext = 0;
        
        this.in = null;
        this.inNext = this.inEnd = 0;
        this.respParser = null;
        this.resp = null;
    }
    
    public String layers() { return "connect-" + base.layers(); }
    
    public ConnectConf configuration() { return conf; }

    public boolean isBlocking() {
        return base.isBlocking();
    }
    
    public ConnectState connect()
    throws IOException {
        advanceConnect();
        if (sh != null) sh.updateInterestOps();
        return state;
    }
    
    public RemoteInfo remote() {
        return base.remote();
    }
    
    public void close()
    throws IOException {
        base.close();
        state = ConnectState.CLOSED;
        round = 0;
        clear();
        in = null;
        inNext = inEnd = 0;
    }
    
    protected void clear() {
        req = null;
        out = null;
        outNext = 0;
        respParser = null;
        resp = null;
    }
    
    protected HttpRequestPreface buildRequest(final InetSocketAddress address){
        final HttpRequestPreface req = new HttpRequestPreface(
            "CONNECT",
            //address.getAddress().getHostAddress()
            address.getHostName() + ":" + address.getPort(),
            1, 1);
        if (conf.keepAlive.has() && conf.keepAlive.get())
            req.addHeader("Proxy-Connection", "keep-alive");
        if (conf.userAgent.has())
            req.addHeader("User-Agent", conf.userAgent.get());
        return req;
    }
    
    protected void advanceConnect()
    throws IOException {
        if (ConnectState.CONNECTING.compareTo(state) < 0) return;
        state = ConnectState.CONNECTING;
        final ConnectState baseState = base.connect();
        if (baseState == ConnectState.CONNECTED) {
            try {
                while (stepConnect() && state == ConnectState.CONNECTING);
                if (state != ConnectState.CONNECTING) clear();
            } catch (Throwable e) {
                state = ConnectState.DISCONNECTED;
                clear();
                throw Exceptions.uncheckedOr(IOException.class, e);
            }
        } else if (baseState != ConnectState.CONNECTING) {
            state = baseState;
        }
    }
    
    protected boolean isSendRound() {
        return (round & 1) == 0;
    }
    
    protected boolean stepConnect()
    throws IOException, GeneralSecurityException {
        return isSendRound() ? send() : receive();
    }
    
    protected boolean send()
    throws IOException, GeneralSecurityException {
        if (out == null) {
            if (req == null) req = buildRequest(trans.targetAddress);
            if (round == 0) trans.auth.init(req);
            else {
                trans.auth.clean(req);
                trans.auth.responseTo(resp, req);
            }
            out = Charsets.latin1(req.toString());
            outNext = 0;
        }
        final int w = base.output().write(out, outNext, out.length-outNext);
        if (w == 0) return false;
        outNext += w;
        if (outNext == out.length) {
            out = null;
            outNext = 0;
            round++;
        }
        return true;
    }
    
    protected boolean receive()
    throws IOException {
        if (respParser == null) {
            respParser = new HttpResponsePrefaceParser(
                new HttpListener<HttpResponsePreface>() {
                    public void preface(HttpResponsePreface pre) {
                        ConnectLine.this.resp = pre;
                    }
                    public void body(byte[] data, int off, int len) {}
                    public void done() {}
                });
        }
        if (in == null) {
            in = new byte[1024];
            inNext = inEnd = 0;
        }
        if (inNext < inEnd) {
            final int n = respParser.parse(false, in, inNext, inEnd-inNext);
            if (n > 0) inNext += n; 
            if (respParser.isDone()) {
                respParser.reset();
                analyzeResponse();
                return true;
            }
            if (n > 0) return true;
        }
        if (inEnd == in.length) {
            if (inNext < (in.length/2)) {
                final byte[] newin = new byte[2*in.length];
                System.arraycopy(in, inNext, newin, 0, inEnd-inNext);
                in = newin;
            } else {
                System.arraycopy(in, inNext, in, 0, inEnd-inNext);
            }
            inEnd -= inNext;
            inNext = 0;
        }
        final int n = base.input().read(in, inEnd, in.length-inEnd);
        if (n > 0) {
            inEnd += n;
            return true;
        } else {
            if (n < 0) state = ConnectState.DISCONNECTED;
            return false;
        }
    }
    
    protected void analyzeResponse()
    throws IOException {
        final int sc = resp.getStatusCode();
        if (sc == 200) {
            state = ConnectState.CONNECTED;
        } else if (sc == 407) {
            round++;
        } else {
            throw new IOException("Unexpected status code " + sc);
        }
    }
    
    public IStream input()
    throws IOException {
        return new ConnectIStream();
    }
    
    public OStream output()
    throws IOException {
        return new ConnectOStream();
    }
    
    public SelectionHandler register(final SelectionHandler handler) {
        sh = handler;
        handler.addSelectable(this);
        handler.addInterestInterceptor(this);
        base.register(handler);
        return handler;
    }
    
    public int interestOps(final int current) {
        return (state != ConnectState.CONNECTING) ? current
            : isSendRound() ? SelectionKey.OP_WRITE
            : SelectionKey.OP_READ;
    }
    
    public String toString() {
        return "ConnectLine[base: " + base + "]";
    }
    
    
    //======================================================================
    protected class ConnectIStream extends IStream.Defaults {
        
        protected final IStream isbase;
        
        public ConnectIStream()
        throws IOException {
            this.isbase = base.input();
        }
        
        public int available()
        throws IOException {
            return (inNext < inEnd) ? (inEnd - inNext) : isbase.available();
        }
            
        public int read(final byte[] bs, final int off, final int len)
        throws IOException {
            if (in == null) return isbase.read(bs, off, len);
            else {
                int tocopy = Math.min(len, inEnd-inNext);
                System.arraycopy(in, inNext, bs, off, tocopy);
                inNext += tocopy;
                if (inNext == inEnd) {
                    inNext = inEnd = 0;
                    in = null;
                }
                if (tocopy < len) {
                    final int r = isbase.read(bs, off+tocopy, len-tocopy);
                    if (r > 0) tocopy += r;
                }
                return tocopy;
            }
        }
    }
    
    
    //======================================================================
    public class ConnectOStream extends OStream.Defaults {
        
        protected final OStream osbase;
        
        public ConnectOStream()
        throws IOException {
            this.osbase = base.output();
        }
    
        public int write(final byte[] bs, final int off, final int len)
        throws IOException {
            return osbase.write(bs, off, len);
        }
    
        public int flush()
        throws IOException {
            return osbase.flush();
        }
    }
}
