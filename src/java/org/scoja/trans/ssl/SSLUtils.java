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
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class SSLUtils {

    public static TrustManager[] loadTrusts(final File ksfile)
    throws IOException, GeneralSecurityException {
        return loadTrusts(ksfile, null);
    }
    
    public static TrustManager[] loadTrusts(final File ksfile,
            final char[] passwd)
    throws IOException, GeneralSecurityException {
        return getTrusts(loadKeyStore(ksfile, passwd));
    }
    
    public static TrustManager[] getTrusts(final KeyStore ks)
    throws GeneralSecurityException {
        final TrustManagerFactory tmf
            = TrustManagerFactory.getInstance("PKIX");
        tmf.init(ks);
        return tmf.getTrustManagers();
    }
    
    public static KeyManager[] loadKeys(final File ksfile,
            final char[] passwd)
    throws IOException, GeneralSecurityException {
        return getKeys(loadKeyStore(ksfile, passwd), passwd);
    }
    
    public static KeyManager[] getKeys(final KeyStore ks, final char[] passwd)
    throws GeneralSecurityException {
        final KeyManagerFactory kmf
            = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passwd);
        return kmf.getKeyManagers();
    }
    
    public static KeyStore loadKeyStore(final File ksfile,
            final char[] passwd)
    throws IOException, GeneralSecurityException {
        InputStream in = null;
        try {
            in = new FileInputStream(ksfile);
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(in, passwd);
            return ks;
        } finally {
            if (in != null) 
                try {in.close();} catch (Throwable ignored) {}
        }
    }
    
    public static KeyStore loadKeyStoreForUnprivileged(final File ksfile,
            final char[] passwd)
    throws Exception {
        return AccessController.doPrivileged(
            new PrivilegedExceptionAction<KeyStore>() {
            public KeyStore run() throws Exception {
                return loadKeyStore(ksfile, passwd);
            }
        });
    }
}
