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
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.scoja.cc.lang.Exceptions;

import org.scoja.trans.Transport;
import org.scoja.trans.TransportLine;
import org.scoja.trans.lc.LCLine;

/**
 * <p><b>Sun JCCE supported protocols</b>
 * Sun JCCE supports the following protocols: SSL, SSLv3, TLS, TLSv1.
 *
 * <p><b>Connection state</b>
 * A transport line is not fully connected until SSL handshake is not done.
 * From the beginning of the handshake (included) until the end (excluded)
 * the transport line is in CONNECTING state.
 *
 * <p><b>SSL close</p>
 * Currently there is no way to proceed to a negotiated close.
 * TransportLine and ConnectionState should be extended.
 *
 * @todo FIXME
 * SSLContext must be nulled if any of the configuration properties used to
 * configure it changes.
 */
public class SSLTransport implements Transport<SSLConf> {

    protected final String protocol;
    protected final Transport base;
    protected final SSLConf.Stacked conf;
    protected SSLContext ctxt;
    protected SSLService serv;

    public static SSLTransport tls(final Transport base) {
        return tls10(base);
    }
    
    public static SSLTransport tls10(final Transport base) {
        return new SSLTransport("TLSv1", base);
    }
    
    public static SSLTransport ssl(final Transport base) {
        return ssl3(base);
    }
    
    public static SSLTransport ssl3(final Transport base) {
        return new SSLTransport("SSLv3", base);
    }
        
    public SSLTransport(final String protocol, final Transport base) {
        this.protocol = protocol;
        this.base = base;
        this.conf = new SSLConf.Stacked();
        this.ctxt = null;
        this.serv = null;
    }
    
    public boolean isBlocking() { return base.isBlocking(); }
    
    public String layers() { return "ssl-" + base.layers(); }
    
    public String endPointId() { return base.endPointId(); }
    
    public SSLConf configuration() { return conf; }
    
    protected SSLContext getContext()
    throws IOException {
        if (ctxt == null) ctxt = createContext();
        return ctxt;
    }
    
    protected SSLContext createContext()
    throws IOException {
        try {
            final SSLContext ctxt = SSLContext.getInstance(protocol);
            final KeyManager[] kms = conf.getKeyManagers().get(null);
            final TrustManager[] tms = conf.getTrustManagers().get(null);
            final SecureRandom rnd = conf.getSecureRandom().get(null);
            ctxt.init(kms, tms, rnd);
            return ctxt;
        } catch (GeneralSecurityException e) {
            throw Exceptions.initCause(
                new IOException("Cryptography error: " + e.getMessage()), e);
        }
    }
    
    protected SSLEngine createEngine(final SSLConf.Stacked conf)
    throws IOException {
        final SSLEngine eng = getContext().createSSLEngine();
        if (conf.getProtocols().has())
            eng.setEnabledProtocols(conf.getProtocols().get());
        if (conf.getCipherSuites().has())
            eng.setEnabledCipherSuites(conf.getCipherSuites().get());
        switch (conf.getClientAuth().get(SSLClientAuthenticationMode.IGNORE)) {
        case REQUEST: eng.setWantClientAuth(true); break;
        case REQUIRE: eng.setNeedClientAuth(true); break;
        }
        return eng;
    }

    public TransportLine<SSLConf> newClient() {
        return new LCLine<SSLConf>(new SSLLine.Client(this));
    }
    
    public SSLService server() {
        if (serv == null) serv = new SSLService(this);
        return serv;
    }
    
    public String toString() {
        return "SSLTransport[on: " + base
            + ", with: " + conf + "]";
    }
}
