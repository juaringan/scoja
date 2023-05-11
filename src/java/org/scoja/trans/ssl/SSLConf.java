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

import java.security.SecureRandom;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.scoja.cc.lang.Maybe;

public interface SSLConf {

    public Maybe<String[]> getProtocols();
    public void setProtocols(String[] protocols);

    public Maybe<String[]> getCipherSuites();
    public void setCipherSuites(String[] suites);
    
    public Maybe<SSLClientAuthenticationMode> getClientAuth();
    public void setClientAuth(SSLClientAuthenticationMode mode);
    
    public Maybe<KeyManager[]> getKeyManagers();
    public void setKeyManagers(KeyManager[] kms);
    
    public Maybe<TrustManager[]> getTrustManagers();
    public void setTrustManagers(TrustManager[] tms);
    
    public Maybe<SecureRandom> getSecureRandom();
    public void setSecureRandom(SecureRandom random);
    
    
    //======================================================================
    public static abstract class Skeleton implements SSLConf {
        public String toString() {
            return "SSLConf["
                + "protocols: " + getProtocols()
                + ", cipher suites: " + getCipherSuites()
                + ", client authentication: " + getClientAuth()
                + ", key managers: " + getKeyManagers()
                + ", trust managers: " + getTrustManagers()
                + ", secure random: " + getSecureRandom()
                + "]";
        }
    }
    
    
    //======================================================================
    public static class Stacked extends Skeleton {
    
        protected final Maybe.Mutable<String[]> protocols;
        protected final Maybe.Mutable<String[]> cipherSuites;
        protected final Maybe.Mutable<SSLClientAuthenticationMode> clientAuth;
        protected final Maybe.Mutable<KeyManager[]> keyManagers;
        protected final Maybe.Mutable<TrustManager[]> trustManagers;
        protected final Maybe.Mutable<SecureRandom> secureRandom;
        
        public Stacked() {
            this(null);
        }
        
        public Stacked(final SSLConf base) {
            if (base == null) {
                this.protocols = Maybe.Stacked.undef();
                this.cipherSuites = Maybe.Stacked.undef();
                this.clientAuth = Maybe.Stacked.undef();
                this.keyManagers = Maybe.Stacked.undef();
                this.trustManagers = Maybe.Stacked.undef();
                this.secureRandom = Maybe.Stacked.undef();
            } else {
                this.protocols = base.getProtocols().push();
                this.cipherSuites = base.getCipherSuites().push();
                this.clientAuth = base.getClientAuth().push();
                this.keyManagers = base.getKeyManagers().push();
                this.trustManagers = base.getTrustManagers().push();
                this.secureRandom = base.getSecureRandom().push();
            }
        }
        
        public Maybe<String[]> getProtocols() { return protocols; }
        public void setProtocols(final String[] protocols) {
            this.protocols.set(protocols);
        }
        
        public Maybe<String[]> getCipherSuites() { return cipherSuites; }
        public void setCipherSuites(final String[] suites) {
            this.cipherSuites.set(suites);
        }
    
        public Maybe<SSLClientAuthenticationMode> getClientAuth() {
            return clientAuth; }
        public void setClientAuth(final SSLClientAuthenticationMode mode) {
            this.clientAuth.set(mode);
        }
    
        public Maybe<KeyManager[]> getKeyManagers() { return keyManagers; }
        public void setKeyManagers(final KeyManager[] kms) {
            this.keyManagers.set(kms);
        }
        
        public Maybe<TrustManager[]> getTrustManagers() {return trustManagers;}
        public void setTrustManagers(final TrustManager[] tms) {
            this.trustManagers.set(tms);
        }
    
        public Maybe<SecureRandom> getSecureRandom() { return secureRandom; }
        public void setSecureRandom(final SecureRandom random) {
            this.secureRandom.set(random);
        }
    }
}
