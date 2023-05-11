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
package org.scoja.trans;

import java.net.InetSocketAddress;
import java.net.URI;
import java.io.FileInputStream;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.security.KeyStore;
import javax.net.ssl.TrustManagerFactory;

import org.scoja.cc.ntlm.NTLMClient;
import org.scoja.cc.minihttp.HttpClientAuth;
import org.scoja.cc.minihttp.HttpNoClientAuth;
import org.scoja.cc.minihttp.HttpBasicAuth;
import org.scoja.cc.minihttp.NTLMOnHttpClient;

import org.scoja.trans.tcp.TCPConf;
import org.scoja.trans.tcp.TCPTransport;
import org.scoja.trans.nbtcp.NBTCPTransport;
import org.scoja.trans.connect.ConnectTransport;
import org.scoja.trans.ssl.SSLTransport;

/**
 * This is a trivial class to make an HTTP request with the transport layer.
 * <p>
 * It needs an URI.
 * The URI protocol encodes part of the transport stack:
 * it must be <tt>http</tt> followed by 
 * `<tt>s</tt>' (to request ssl)
 * and/or `<tt>l</tt>' (to request non-blocking (lazy) sockets).
 * <p>
 * If a second argument is given, it is supposed to be the proxy address.
 * <p>
 * If a third and fourth argument, they are suppose to be the user and password
 * to negotiate an NTLM authentication.
 */
public class HttpRequest {

    public static void main(final String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println(
                "java [-Dkeystore=<jks>] HttpRequest"
                + " http[s][l]://<host>[:<port>][/<path>]"
                + " [<proxyhost>:<proxyport> [<authtype> <user> <password>]");
            System.exit(-1);
        }
        final URI url = new URI(args[0]);
        final String sch = url.getScheme();
        if (!sch.startsWith("http")) throw new IllegalArgumentException(
            "Only http[s][l] supported");
        boolean secure = false, lazy = false;
        for (int i = 4; i < sch.length(); i++) {
            final char s = sch.charAt(i);
            if (s == 's') secure = true;
            else if (s == 'l') lazy = true;
            else throw new IllegalArgumentException(
                "Only http[s][l] supported");
        }
        InetSocketAddress proxy = null;
        if (args.length >= 2) {
            final String[] pieces = args[1].split(":");
            if (pieces.length != 2) throw new IllegalArgumentException(
                "Proxy address must be <proxyhost>:<proxyport>");
            proxy = new InetSocketAddress(
                pieces[0], Integer.parseInt(pieces[1]));
        }
        String authtype = null, user = null, password = null;
        if (args.length >= 5) {
            authtype = args[2];
            user = args[3];
            password = args[4];
        }
        
        final InetSocketAddress hostAddr
            = new InetSocketAddress(url.getHost(), url.getPort());

        final InetSocketAddress connAddr = (proxy != null) ? proxy : hostAddr;
                        
        Transport<?> trans;
        {
            final Transport<TCPConf> base = !lazy ? new TCPTransport(connAddr)
                : new NBTCPTransport(connAddr);
            base.configuration().setReceiveBufferSize(16*1024);
            System.err.println("Base configuration: " + base.configuration());
            trans = base;
        }
        if (proxy != null) {
            final HttpClientAuth auth;
            if (authtype == null) {
                auth = new HttpNoClientAuth(true);
            } else if ("basic".equals(authtype)) {
                auth = new HttpBasicAuth(true, user, password);
            } else if ("ntlm".equals(authtype)) {
                final NTLMClient ntlm = new NTLMClient(user, password);
                ntlm.setTarget("");
                ntlm.resolveHost();
                //ntlm.setDomain(null);
                auth = new NTLMOnHttpClient(ntlm, true);
            } else {
                throw new IllegalArgumentException(
                    "Unsupported auth type `" + authtype + "'");
            }
            final ConnectTransport conn
                = new ConnectTransport(trans, hostAddr, auth);
            trans = conn;
        }
        if (secure) {
            final SSLTransport ssl = SSLTransport.tls10(trans);
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(System.getProperty("keystore")), null);
            final TrustManagerFactory tmf
                = TrustManagerFactory.getInstance("PKIX");
            tmf.init(ks);
            ssl.configuration().setTrustManagers(tmf.getTrustManagers());
            trans = ssl;
        }
        
        final TransportLine<?> line = trans.newClient();
        System.err.println("Line: " + line);
        System.err.println("State after new client: " + line.connectState());
        System.err.println("Line configuration: " + line.configuration());
        
        final Selector selector = Selector.open();
        final SelectionHandler sh
            = line.register(new SelectionHandler(selector,null));
        System.err.println("Selector keys: " + selector.keys());
        try {
            for (int i = 0; i < 25; i++) {
                final ConnectState state = line.connect();
                System.err.println("State: " + state);
                System.err.println("Selector keys: " + selector.keys());
                if (state != ConnectState.CONNECTING) break;
                if (lazy) {
                    final int n = selector.select();
                    System.err.println("Ready ops: " + selector.selectedKeys()
                            .iterator().next().readyOps());
                }
            }
        } catch (Throwable e) {
            System.err.println("Connection error: " + e);
            e.printStackTrace(System.err);
        }
        System.err.println("State after not connecting: "+line.connectState());
        if (line.connectState() != ConnectState.CONNECTED) return;

        final StringBuilder sb
            = new StringBuilder("GET " + url.getPath() + " HTTP/1.1\r\n");
        /*
        for (int i = 0; i < 1000; i++) {
            sb.append("X-Header-").append(i)
                .append(": X Header with a silly content\r\n");
        }
        */
        sb.append("\r\n");
        final byte[] req = sb.toString().getBytes();
        sh.interestOps(SelectionKey.OP_WRITE);
        int n = 0;
        while (n < req.length) {
            if (lazy) selector.select();
            final int m = line.output().write(req, n, req.length-n);
            System.err.println("Request block send " + m);
            if (m < 0) break;
            n += m;
        }
        System.err.println("Request send: " + n + " out of " + req.length);
        sh.interestOps(SelectionKey.OP_READ);
        final byte[] resp = new byte[1024];
        for (;;) {
            if (lazy) selector.select();
            n = line.input().read(resp, 0, resp.length);
            if (n < 0) break;
            System.out.write(resp, 0, n);
            System.out.flush();
        }
        System.out.println();
    }
}
