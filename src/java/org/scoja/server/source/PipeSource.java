/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
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
package org.scoja.server.source;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.scoja.trans.RemoteInfo;
import org.scoja.server.core.ClusterSkeleton;
import org.scoja.server.core.DecoratedLink;
import org.scoja.server.core.Link;
import org.scoja.server.core.Linkable;
import org.scoja.server.parser.EventSplitter;

public class PipeSource 
    extends ClusterSkeleton
    implements DecoratedLink, Runnable {
    
    protected final String pipeName;
    protected final Link link;
    protected final RemoteInfo localHost;
    protected final EventSplitter splitter;
    protected final byte[] buffer;
    protected InputStream is;
    
    public PipeSource(final String pipeName)
    throws IOException, UnknownHostException {
        this.pipeName = pipeName;
        this.link = new Link();
	this.localHost = new RemoteInfo.Inet(InetAddress.getLocalHost());
        this.splitter = new EventSplitter(localHost, link);
        this.buffer = new byte[1024];
        this.is = null;
        setThreads(1);
    }
    
    public Linkable getLinkable() {
        return link;
    }
    
    public void start() {
        Internal.warning(Internal.SOURCE_PIPE, "Starting " + this);
        try {
            is = new FileInputStream(pipeName);
            super.start();
            super.startAllThreads();
        } catch (IOException e) {
            Internal.err(Internal.SOURCE_PIPE,
                         "Source " + this + " will not listen!", e);
        }
    }
    
    public void shouldStop() {
        super.shouldStop();
        close();
    }
    
    public void run() {
        for (;;) {
            try {
                final int read = is.read(buffer);
                if (read == -1) break;
                splitter.add(buffer, 0, read);
            } catch (Throwable e) {
                if (!canRecover(e)) break;
            }
        }
        splitter.flush();
        close();
        threadStopped();
    }
    
    private synchronized void close() {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                Internal.err(Internal.SOURCE_PIPE,
                             "While closing input stream of " + this, e);
            }
            is = null;
        }
    }
    
    private synchronized boolean canRecover(final Throwable e) {
        if (stopRequested()) return false;
        Internal.err(Internal.SOURCE_PIPE, "At " + this, e);
        return false;
    }
    
    public String toString() {
        return "Pipe source reading from " + pipeName;
    }
    
    public String getName4Thread() {
        return "pipe@" + pipeName;
    }
}
