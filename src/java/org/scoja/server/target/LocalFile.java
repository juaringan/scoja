/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2008  Bankinter, S.A.
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

package org.scoja.server.target;

import java.io.IOException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.OutputStream;
import java.util.Date;

import org.scoja.server.source.Internal;

import org.scoja.io.posix.PosixFile;

public abstract class LocalFile
    <A extends OutputStream, B extends LocalFileSystem>
    extends OutputStream implements BinaryFile {

    protected final B fs;
    protected final String filename;
    protected final FileBuilding fb;
    protected A out;
    protected FileDescriptor outfd;
    protected long reconsiderAt;

    public LocalFile(final B fs,
                     final String filename,
                     final FileBuilding fb) {
        this.fs = fs;
        this.filename = filename;
        this.fb = fb;
        this.out = null;
        this.outfd = null;
        this.reconsiderAt = 0;
    }
    
    public void write(final int b) {
        if (!checkOutput()) return;
        try {
            out.write(b);
        } catch (IOException e) {
            error(e);
        }
    }
    
    public void write(final byte[] b) {
        write(b, 0, b.length);
    }
    
    public void write(final byte[] b, final int off, final int len) {
        if (!checkOutput()) return;
        try {
            out.write(b, off, len);
        } catch (IOException e) {
            error(e);
        }
    }
    
    public void flush() {
        if (out != null) {
            try {
                out.flush();
            } catch (IOException e) {
                error(e);
            }
        }
    }
    
    public void close() {
        if (out != null) {
            try { out.flush(); } catch (IOException ignored) {}
            try { out.close(); } catch (IOException ignored) {}
            out = null;
        }
    }
    
    public OutputStream getOut() { return this; }
    
    public void sync() {
        if (out != null) {
            try {
                outfd.sync();
            } catch (IOException ignored) {}
        }
    }
    
    public String toString() {
        return "LocalOutputStream["
            + "file: " + filename
            + ", attributes: " + fb
            + ((out == null)
               ? (", reconsider in: "
                  + ((reconsiderAt - System.currentTimeMillis()) / 1000.0))
               : ", currently open")
            + "]";
    }
    
    protected void error(final IOException e) {
        close();
        reconsiderAt = fs.error(filename, e);
        Internal.warning(Internal.TARGET_FILE,
                         "All data to file `" + filename
                         + "' will ignored until " + new Date(reconsiderAt));
    }
    
    protected boolean checkOutput() {
        if (out != null) return true;
        if (System.currentTimeMillis() < reconsiderAt) return false;
        try {
            Internal.debug(Internal.TARGET_FILE,
                           "Trying to open `" + filename + "'");
            final PosixFile file
                = new PosixFile(filename, fb.getFileAttributes());
            if (!file.exists()) {
                if (fb.shouldMakeDirectories()) {
                    new PosixFile(file.getParent(),fb.getDirectoryAttributes())
                        .makeDirectories();
                }
                file.createNewFile();
            }
            out = openAppend(file);
            outfd = getFD(out);
            Internal.info(Internal.TARGET_FILE,
                          "File `" + filename + "' successfully open");
            return true;
        } catch (IOException e) {
            error(e);
            return false;
        }
    }
    
    protected abstract A openAppend(File filename) throws IOException;
    protected abstract FileDescriptor getFD(A out) throws IOException;
}
