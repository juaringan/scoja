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

package org.scoja.io.posix;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.scoja.cc.lang.Void;

/**
 * Currently a <i>global variable</i> to store system wide {@link PosixLike}
 * implementation.
 * By default, this variable is filled with a {@link PosixFree}, i.e.
 * with an implementation that do nothing or fail immediately with an
 * exception.
 * If <tt>org.scoja.io.posix.provider</tt> is defined, its value is supposed
 * to be the name of a {@link PosixLike} to use.
 */
public abstract class Posix {

    private static final Logger logger
        = Logger.getLogger(Posix.class.getName());

    public static final String PROVIDER_PROPERTY
        = "org.scoja.io.posix.provider";

    private static final Object lock = new Object();
    private static PosixLike currentPosix = null;

    public static PosixLike getPosix() {
        synchronized (lock) {
            if (currentPosix == null) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() { buildPosix(); return Void.ME; }
                });
            }
            return currentPosix;
        }
    }

    protected static void buildPosix() {    
        final String provider = System.getProperty(PROVIDER_PROPERTY);
        if (provider != null) {
            try {
                currentPosix
                    = (PosixLike)Class.forName(provider).newInstance();
            } catch (Throwable e) {
                logger.log(Level.WARNING, 
                        "While build posix provider from property "
                        + PROVIDER_PROPERTY, e);
            }
        }
        if (currentPosix == null) currentPosix = new PosixFree();
    }
    
    public static void setPosix(final PosixLike newPosix) {
        synchronized (lock) {
            currentPosix = newPosix;
        }
    }
    
    public static void setPosix(final String newPosixClassName)
    throws ClassNotFoundException, InstantiationException,
           IllegalAccessException {
        setPosix((PosixLike)Class.forName(newPosixClassName).newInstance());
    }
    
    /*
    private static PosixSystem instance;
    static {
        try {
            instance = new NativePosixSystem();
        } catch (Throwable e) {
            System.err.println("Failed to load dynamic library: " + e);
            instance = null;
        }
    }
    */
}


