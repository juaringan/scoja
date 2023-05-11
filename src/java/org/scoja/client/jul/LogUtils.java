/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2010  Bankinter, S.A.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser/Library General Public License
 * as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
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
package org.scoja.client.jul;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.LogManager;

public class LogUtils {
    public static String getString(final String key) {
        final LogManager lm = LogManager.getLogManager();        
        return lm.getProperty(key);
    }
    
    public static String getString(final String key, final String def) {
        final String value = getString(key);
        return (value == null) ? def : value;
    }
    
    public static int getNat(final String key, final int def) {
        final String strval = getString(key);
        if (strval == null) return def;
        final int value;
        try {
            value = Integer.parseInt(strval);
        } catch (final NumberFormatException e) {
            return def;
        }
        return (value < 0) ? def : value;
    }
    
    public static int getPort(final String key, final int def) {
        final String strval = getString(key);
        if (strval == null) return def;
        final int value;
        try {
            value = Integer.parseInt(strval);
        } catch (final NumberFormatException e) {
            return def;
        }
        return (value < 0 || value >= (1<<16)) ? def : value;
    }
    
    public static void badValue(final Logger log,
            final String prop, final String value, final Throwable e) {
        log.log(Level.WARNING, "Bad value `" +value+ "' for property `" +prop
                + "': " + e.getMessage(), e);
    }
}
