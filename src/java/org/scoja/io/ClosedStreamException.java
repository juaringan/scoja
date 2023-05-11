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
 * This is a clone of java.nio.ClosedChannelException.
 * It exists only to avoid depending on java.nio.
 * It extends {@link SocketException} so it can be used in context where
 * a socket specific exception is needed instead of a general IOException.
 * But it should be considered a general IO exception.
 */
public class ClosedStreamException
    extends SocketException {
    
    public ClosedStreamException() {
        super("Stream has been closed");
    }
    
    public ClosedStreamException(final String msg) {
        super(msg);
    }
    
    public ClosedStreamException(final Throwable cause) {
        this();
        initCause(cause);
    }
    
    public ClosedStreamException(final String msg, final Throwable cause) {
        this(msg);
        initCause(cause);
    }
}
