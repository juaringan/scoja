/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2010  Mario Mart�nez
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

import java.io.IOException;

public class Errors {

    public static IOException closed(final Object line) {
        return new IOException("Closed (" + line + ")");
    }
    
    public static IOException unconnected(final Object line) {
        return new IOException(
            "Transport line is not connected (" + line + ")");
    }
    
    public static IOException baseConnectionFailed(final Object line) {
        return new IOException(
            "Base transport line could not connect (" + line + ")");
    }
    
    public static IOException cannotReconnect(final Object line) {
        return new IOException(
            "Cannot reconnect after being closed (" + line + ")");
    }    
     
    public static IOException cannotConfigure(final Object elem) {
        return new IOException("Cannot access to configuration");
    }
}
