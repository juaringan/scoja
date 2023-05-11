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

import java.io.File;
import java.io.IOException;

import org.scoja.io.posix.PosixFile;

/**
 * This is the {@link SocketAddress} of Unix domain sockets (aka local IPC).
 * It is just a file name.
 * It mimics {@link InetSocketAddress} (that is,
 * <tt>java.net.InetSocketAddress</tt>) as much as possible.
 */
public class UnixSocketAddress
    extends SocketAddress {

    /** The Unix domain socket file. */
    protected final PosixFile file;
    
    public UnixSocketAddress(final String path) {
        this(new PosixFile(path));
    }
    
    public UnixSocketAddress(final File file) {
        this(new PosixFile(file));
    }
    
    public UnixSocketAddress(final PosixFile file) {
        if (file == null) {
            throw new IllegalArgumentException("A null socket file");
        }
        this.file = file;
    }
    
    public String getPath() {
        return file.getPath();
    }
    
    public PosixFile getFile() {
        return file;
    }

    /**
     * Check wether the socket file exists and is a Unix socket.
     */    
    public boolean isUnresolved() {
        try {
            return !(file.exists() && file.isSocket());
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * It exists, delete this socket file.
     * Return <tt>true</tt> iff some file was deleted.
     */    
    public boolean clear()
    throws IOException {
        return file.exists() ? file.delete() : false;
    }
    
    public void writeAttributes()
    throws IOException {
        file.writeAttributes();
    }
    
    
    //======================================================================
    public String toString() {
        return "UnixSocketAddress[" + file + "]";
    }
    
    public int hashCode() {
        return file.hashCode();
    }
    
    public boolean equals(final Object other) {
        return (other instanceof UnixSocketAddress)
            && equals((UnixSocketAddress)other);
    }
    
    public boolean equals(final UnixSocketAddress other) {
        return this.file.equals(other.file);
    }
}
