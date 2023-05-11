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

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.io.IOException;

import org.scoja.io.posix.Posix;
import org.scoja.io.posix.PosixLike;

/**
 */
public class Selector {

    protected final PosixLike posix;

    protected final Set keys;
    protected final Set roKeys;
    protected final Set selectedKeys;
    protected final Set canceledKeys;
    protected final Object openLock;
    protected final Pipe wakeupPipe;
    protected boolean isOpen;
    protected boolean isSelecting;

    public Selector()
    throws IOException {
        this.posix = Posix.getPosix();
        
        this.keys = new HashSet();
        this.roKeys = Collections.unmodifiableSet(this.keys);
        this.selectedKeys = new HashSet();
        this.canceledKeys = new HashSet();
        this.openLock = new Object();
        this.wakeupPipe = new Pipe();
        this.isOpen = true;
        this.isSelecting = true;
    }
    
    public static Selector open()
    throws IOException {
        return new Selector();
    }
    
    protected void checkOpen(final boolean wanted) {
        if (isOpen != wanted) {
            throw new IllegalStateException("This selector has been closed");
        }
    }
    
    public boolean isOpen() {
        synchronized (openLock) {
            return isOpen;
        }
    }

    public Set keys() {
        synchronized (openLock) {
            checkOpen(true);
            return keys;
        }
    }
    
    public Set selectedKeys() {
        synchronized (openLock) {
            checkOpen(true);
            return selectedKeys;
        }
    }
    
    public int selectNow()
    throws IOException {
        return select(-1);
    }
    
    public int select()
    throws IOException {
        return select(0);
    }
    
    public int select(final long timeout)
    throws IOException {
        synchronized (this) {
            int[] fds;
            int[] interests;
            SelectionKey[] skeys;
            int maxFD = -1;
            int inuse = 0;
            synchronized (openLock) {
                checkOpen(true);
                keys.removeAll(canceledKeys);
                canceledKeys.clear();
                fds = new int[keys.size()+1];
                interests = new int[fds.length];
                skeys = new SelectionKey[fds.length];
                final Iterator it = keys.iterator();
                while (it.hasNext()) {
                    final SelectionKey skey = (SelectionKey)it.next();
                    final int interest = skey.interestOps();
                    if (interest == 0) continue;
                    skeys[inuse] = skey;
                    interests[inuse] = interest;
                    fds[inuse] = skeys[inuse].channel().getFD();
                    //System.err.println("Adding " + fds[inuse] + " to select "
                    //                   + " with interest " + interest);
                    maxFD = Math.max(maxFD, fds[inuse]);
                    inuse++;
                }
                skeys[inuse] = null;
                fds[inuse] = wakeupPipe.getSource().getFD();
                //System.err.println("Adding to select: " + fds[inuse]);
                interests[inuse] = SelectionKey.OP_READ;
                maxFD = Math.max(maxFD, fds[inuse]);
                inuse++;
            }
            isSelecting = true;
            final int selected;
            try {
                selected = posix.select(fds, interests, inuse, maxFD, timeout);
            } finally {
                isSelecting = false;
            }
            synchronized (openLock) {
                checkOpen(true);
                synchronized (selectedKeys) {
                    for (int i = 0; i < inuse-1; i++) {
                        if (interests[i] != 0) {
                            skeys[i].readyOps(interests[i]);
                            selectedKeys.add(skeys[i]);
                        }
                    }
                }
                if (interests[interests.length-1] != 0) {
                    wakeupPipe.getSource().read();
                }
            }
            return selected;
        }
    }
    
    public Selector wakeup() {
        synchronized (openLock) {
            try {
                if (isSelecting) wakeupPipe.getSink().write(0);
            } catch (IOException e) {}
        }
        return this;
    }
    
    public void close() {
        synchronized (openLock) {
            if (!isOpen) return;
            isOpen = false;
        }
        try {
            wakeupPipe.getSink().write(0);
        } catch (IOException e) {}
        final Iterator it = keys.iterator();
        while (it.hasNext()) {
            ((SelectionKey)it.next()).cancel();
        }
        keys.clear();
        //canceledKeys.clear();
        //selectedKeys.clear();
     }
    
    protected void addKey(final SelectionKey key) {
        synchronized (openLock) {
            checkOpen(true);
            keys.add(key);
        }
    }
        
    protected void cancelKey(final SelectionKey key) {
        synchronized (openLock) {
            if (!isOpen) return;
            canceledKeys.add(key);
        }
    }
    
    //======================================================================
    protected void finalize() {
        close();
    }
    
    public synchronized String toString() {
        return getClass().getName()
            + "[keys: " + keys
            + ", selected keys: " + selectedKeys
            + ", canceled keys: " + canceledKeys
            + "]";
    }
}
