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

package org.scoja.io;

import java.net.SocketException;

/**
 * Raised by I/O operations not supported in the current platform.
 * We avoid raising UnsupportedOperationException because it is annoying
 * to mix checked and unchecked exceptions: it is usual to think that
 * caching all checked exception is enough to control all errors.
 */
public class UnsupportedIOException
    extends SocketException {
    
    public UnsupportedIOException() {
        super("This IO operation is not supported on this platform");
    }
    
    public UnsupportedIOException(final String msg) {
        super(msg);
    }
    
    public UnsupportedIOException(final Throwable cause) {
        this();
        initCause(cause);
    }
    
    public UnsupportedIOException(final String msg, final Throwable cause) {
        this(msg);
        initCause(cause);
    }
}
