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

/**
 * This is flushing information.
 * There are several methods:
 * <dl>
 * <dt>{@link #BUFFER}
 * <dd>Data is written when the buffer is full.
 * <dt>{@link #FLUSH}
 * <dd>Data is flushed after each event.
 * <dt>{@link #SYNC}
 * <dd>Data is flushed and then synchronized after each event.
 * </dl>
 * These methods are configured with 2 pararameters (not always having
 * efect or the same effect): <i>allowedDelay</i> and <i>bufferSize</i>.
 * <p>
 * When method is {@link #BUFFER}, <i>allowedDelay</i> has no effect
 * and <i>bufferSize</i> must be a positive integer that will be used
 * as the size of the buffer.
 * <p>
 * When method is {@link #FLUSH} or {@link #SYNC},
 * <i>allowedDelay</i> is the number of events that must be written to a file
 * before executing a flush (or sync).
 * If <i>bufferSize</i> is positive number, it is used as the buffer size;
 * if it is a negative number, its oposite is used as the initial size of an
 * unlimited stretchable buffer.
 */
public class Flushing {

    public static final int BUFFER = 0;
    public static final int FLUSH = 1;
    public static final int SYNC = 2;
    
    protected final int method;
    protected final int after;
    protected final int bufferSize;
    
    private static final Flushing defaultInstance
        = new Flushing(FLUSH, 1, 0);
        
    public static Flushing getDefault() {
        return defaultInstance;
    }
    
    public Flushing(final int method,
                    final int allowedDelay, final int bufferSize) {
        this.method = method;
        this.after = allowedDelay;
        this.bufferSize = bufferSize;
    }
    
    public int getMethod() {
        return method;
    }
    
    public int getAllowedDelay() {
        return after;
    }
    
    public int getBufferSize() {
        return bufferSize;
    }
    
    public void flush(final TextFile file) {
        switch (method) {
        case BUFFER: break;
        case FLUSH: file.flush(after); break;
        case SYNC: file.sync(after); break;
        }
    }
}
