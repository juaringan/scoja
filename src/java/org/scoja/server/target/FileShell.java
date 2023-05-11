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

package org.scoja.server.target;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import org.scoja.cc.io.StreamSpy;

import org.scoja.util.LRUShell;
import org.scoja.io.posix.PosixFile;
import org.scoja.server.source.Internal;

/**
 * 
 */
public class FileShell {

    protected final String filename;
    protected final LRUShell lruShell;
    protected final FileLRUCache cache;

    protected FileOutputStream outStream;
    protected FileDescriptor outFD;
    protected StreamSpy.Counter counter;  
    protected OutputStream outCounter;
    protected PrintWriter outPrinter;
    protected int delayed;
    
    public FileShell(final String filename,
                     final LRUShell lruShell,
                     final FileLRUCache cache) {
        this.filename = filename;
        this.lruShell = lruShell;
        this.cache = cache;
        this.outStream = null;
        this.outFD = null;
        this.counter = null;
        this.outCounter = null;
        this.outPrinter = null;
        this.delayed = 0;
    }
    
    public boolean ensureFileEntry(final FileBuilding fb,
                                   final Flushing flushing)
    throws IOException {
        if (outStream != null) return false;
        try {
            final PosixFile file
                = new PosixFile(filename, fb.getFileAttributes());
            if (!file.exists()) {
                if (fb.shouldMakeDirectories()) {
                    new PosixFile(file.getParent(),fb.getDirectoryAttributes())
                        .makeDirectories();
                }
                file.createNewFile();
            }
            this.outStream = new FileOutputStream(file, true);
            this.outFD = this.outStream.getFD();
            //No IOException in the remaining code.
            this.counter = new StreamSpy.Counter();
            this.outCounter
                = new StreamSpy.Output(this.outStream, this.counter);
            final int method = flushing.getMethod();
            final int bufferSize = flushing.getBufferSize();
            Writer writer = new OutputStreamWriter(this.outCounter);
            if (bufferSize > 0) {
                writer = new BufferedWriter(writer, bufferSize);
            }
            this.outPrinter = new PrintWriter(writer);
            lruShell.put(this);
            Internal.notice(Internal.TARGET_FILE, "File \"" +filename+ "\""
                            + " added to the open files cache");
            return true;
        } catch (IOException e) {
            //e.printStackTrace(System.err);
            failedWhileOpening();
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    
    protected void failedWhileOpening() {
        if (outStream != null) {
            try { outStream.close(); } catch (IOException e) {}
            outStream = null;
        }
        cache.failedWhileOpening(filename);
    }
    
    public long length() {
        return counter.getBytesWritten();
    }
    
    public OutputStream getOutputStream() {
        return outCounter;
    }
    
    public PrintWriter getPrintWriter() {
        return outPrinter;
    }
    
    public void flush()
    throws IOException {
        outPrinter.flush();
    }
    
    public void flush(final Flushing flushing)
    throws IOException {
        delayed++;
        if (flushing.getMethod() != Flushing.BUFFER
            && delayed >= flushing.getAllowedDelay()) {
            outPrinter.flush();
            if (flushing.getMethod() == Flushing.SYNC) {
                outFD.sync();
            }
            delayed = 0;
        }
    }
    
    public void release() {
        lruShell.release();
    }
    
    public void expired() {
        outPrinter.close();
        //FIXME: Calling Internal at this method is very dangerous if
        // Internal hasn't its own queue/thread.
        Internal.notice(Internal.TARGET_FILE, "File \"" +filename+ "\""
                        + " removed from the open files cache");
    }
}
