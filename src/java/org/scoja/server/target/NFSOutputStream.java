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

import java.io.OutputStream;

import org.scoja.server.cache.BufferResizer;

public class NFSOutputStream extends OutputStream implements BinaryFile {

    protected final NFSFileSystem fs;
    protected final String filename;
    protected final FileBuilding fb;
    protected final byte[] oneChar;
    
    protected final Object lock;
    protected int incarnation;
    protected NFSFile file;
    protected long offset;
    
    protected boolean waiting;
    protected long reconsiderAt;
    protected boolean closePending;
    protected boolean syncPending;
    protected byte[] buffer;
    protected int bufferLength;
    protected int init;
    protected int z0end;
    protected int z1end;
    protected int end;
    protected int z0written;
    protected int z1written;
    
    protected long writesDropped;
    protected long bytesDropped;
    
    public NFSOutputStream(final NFSFileSystem fs,
                           final String filename,
                           final FileBuilding fb) {
        this.fs = fs;
        this.filename = filename;
        this.fb = fb;
        this.oneChar = new byte[1];
        
        this.lock = new Object();
        this.incarnation = 0;
        this.file = null;
        this.offset = -1;
        
        this.waiting = false;
        this.reconsiderAt = 0;
        this.closePending = this.syncPending = false;
        this.buffer = null;
        this.bufferLength = this.init = this.z0end = this.z1end = this.end = 0;
        this.z0written = this.z1written = 0;
        this.writesDropped = this.bytesDropped = 0;
        doPending();
    }
    
    protected void setReconsiderAt(final long at) {
        this.reconsiderAt = at;
    }
    
    protected long getReconsiderAt() { return reconsiderAt; }

    protected void pendingAsSoonAsPossible0() {
        waiting = true;
        fs.asSoonAsPossible(this);
    }
    
    protected void pendingWhenReconsidered0() {
        waiting = true;
        fs.whenReconsidered(this);
    }
    
    protected void doPending() {
        synchronized (lock) {
            waiting = false;
            doPending0();
        }
    }
    
    protected void doPending0() {
        if (file == null) {
            open();
            return;
        }
        if (z1end < end && !waiting) sendBuffered0();
        if (syncPending && !waiting) sync0();
        if (closePending) close0();
    }
        
    protected void open() {
        if (waiting || fs.shouldWait()) {
            pendingAsSoonAsPossible0();
        } else {
            fs.open(filename, fb, new NFSCallBack<NFSFile>() {
                public void ok(final NFSFile file) { openOK(file); }
                public void fail(final Exception e) { openFail(e); }
            });
        }
    }
    
    protected void openOK(final NFSFile file) {
        System.err.println("File `" + filename + "' successfully open");
        synchronized (lock) {
            incarnation++;
            this.file = file;
            offset = file.getSize();
            z0written = z1written = 0;
            doPending0();
        }
    }
    
    protected void openFail(final Exception e) {
        System.err.println(
            "Open of `" + filename + "' failed: " + e.getMessage());
        synchronized (lock) {
            pendingWhenReconsidered0();
        }
    }
    
    protected void sendBuffered() {
        synchronized (lock) {
            if (file == null) return;
            sendBuffered0();
        }
    }
    
    protected void sendBuffered0() {
        if (waiting || fs.shouldWait()) {
            pendingAsSoonAsPossible0();
        } else {
            final int toSend = Math.min(fs.preferredWrite(), end-z1end);
            if (toSend > 0) {
                sendBuffered0(incarnation, z1end, toSend);
                z1end += toSend;
                if (init == z0end) z0end = z1end;
            }
        } 
    }
    
    protected void sendBuffered0(final int myIncarnation, final int at0,
                                 final int toSend) {
        final long at = offset + at0;
        //System.err.println(
        //    "Writting at " + at + " from " + at0 + "-" + toSend);
        fs.write(file, at, buffer, at0, toSend,
                 new NFSCallBack<Integer>() {
            public void ok(final Integer n) {
                sendOK(myIncarnation, at, toSend, n);
            }
            public void fail(final Exception e) {
                sendFail(myIncarnation, at, toSend, e);
            }
        });
    }
    
    protected void sendOK(final int myIncarnation, final long at,
                          final int toWrite, final int written) {
        if (written != toWrite) {
            System.err.println("Partial write: " + toWrite + " " + written);
        }
        synchronized (lock) {
            if (myIncarnation != incarnation) return;
            final int at0 = (int)(at - offset);
            if (at0 < init) return;
            if (at0 < z0end) {
                z0written += written;
                if (z0written == z0end - init) {
                    if (z1written == z1end - z0end) {
                        init = z0end = z1end;
                        z0written = z1written = 0;
                    } else {
                        init = z0end;
                        z0end = z1end;
                        z0written = z1written;
                        z1written = 0;
                    }
                    final int newlen = fs.getResizer().newLength(
                        bufferLength, init, end, 0);
                    if (newlen < bufferLength) resize(newlen);
                }
            } else {
                z1written += written;
            }
            //System.err.println(
            //    "Send OK " + offset + " " + init + " " + z0end
            //    + " [" + z0written + "] " + z1end + " [" + z1written
            //    + "] " + end);
            if (file == null) return;
            if (written < toWrite) {
                sendBuffered0(myIncarnation, at0+written, toWrite-written);
            }
            doPending0();
        }
    }
    
    protected void sendFail(final int myIncarnation, final long at,
                            final int toWrite, final Exception e) {
        System.err.println(
            "Writing at " + filename + "[" + at + "+" + toWrite + "] failed"
            + ": " + e.getMessage());
        synchronized (lock) {
            if (myIncarnation != incarnation) return;
            this.file = null;
            pendingWhenReconsidered0();
        }
    }


    //----------------------------------------------------------------------
    protected void store(final byte[] b, final int off, final int len) {
        synchronized (lock) {
            if (bufferLength - end < len) {
                final int newlen = fs.getResizer().newLength(
                    bufferLength, init, end, len);
                resize(Math.max(bufferLength,newlen));
            }
            if (bufferLength - end < len) {
                writesDropped++;
                bytesDropped += len;
                System.err.println(
                    "Dropped total " + writesDropped + " " + bytesDropped);
            } else {
                System.arraycopy(b,off, buffer,end,len);
                end += len;
            }
        }
    }
    
    private void resize(final int newlen) {
        buffer = fs.getResizer().resized(buffer, init, end, newlen);
        bufferLength = (buffer == null) ? 0 : buffer.length;
        offset += init;
        z0end -= init;
        z1end -= init;
        end -= init;
        init = 0;
    }
    
    
    //----------------------------------------------------------------------
    
    public void write(final int b) {
        oneChar[0] = (byte)b;
        write(oneChar);
    }
    
    public void write(final byte[] b) {
        write(b, 0, b.length);
    }
    
    public void write(final byte[] b, final int off, final int len) {
        store(b, off, len);
        sendBuffered();
    }
    
    public void flush() {
        //Nothing to do: this class is always flushing.
    }
    
    protected void sync0() {
        if (waiting || fs.shouldWait()) {
            syncPending = true;
            pendingAsSoonAsPossible0();
            return;
        }
        syncPending = false;
        fs.sync(file, null);
    }
    
    public void sync() {
        synchronized (lock) {
            sync0();
        }
    }

    protected void close0() {    
        //System.err.println(
        //    "Close0 " + filename + " " + (init < end) + " " + syncPending);
        if (init < end || syncPending) {
            closePending = true;
            return;
        }
        closePending = false;
        file = null;
        fs.getSpaceControl().resizing(bufferLength,0);
        buffer = null;
        offset = init = z0end = z1end = end = bufferLength = 0;
        fs.close(this);
    }
    
    public void close() {
        synchronized (lock) {
            close0();
        }
    }
    
    public OutputStream getOut() { return this; }
    
    public String toString() {
        synchronized (lock) {
            return "NFSOutputStream["
                + "file: " + filename
                + ", attributes: " + fb
                + ", size: " + (offset + end)
                + "]";
        }
    }
}
