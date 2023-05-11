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

import java.util.Iterator;
import java.util.Collections;
import java.util.Set;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.scoja.common.PriorityUtils;
import org.scoja.trans.RemoteInfo;
import org.scoja.io.UnixServerSocket;
import org.scoja.io.UnixSocket;
import org.scoja.io.UnixSocketAddress;
import org.scoja.io.SelectionKey;
import org.scoja.io.Selector;
import org.scoja.io.posix.FileAttributes;
import org.scoja.io.posix.FileMode;
import org.scoja.io.posix.PosixFile;
import org.scoja.server.core.ClusterSkeleton;
import org.scoja.server.core.DecoratedLink;
import org.scoja.server.core.Event;
import org.scoja.server.core.EventContext;
import org.scoja.server.core.Link;
import org.scoja.server.core.Linkable;
import org.scoja.server.core.ScojaThread;
import org.scoja.server.core.Serie;
import org.scoja.server.parser.EventSplitter;
import org.scoja.server.source.Internal;

/**
 */
public class SelectingUnixStreamSource 
    extends ClusterSkeleton
    implements DecoratedLink, Runnable {
    
    protected final Link link;
    
    protected PosixFile file;
    protected boolean keepAlive;
    protected boolean reuseAddress;
    
    protected UnixServerSocket serverSocket;
    protected Selector selector;
    protected final RemoteInfo localHost;
    protected final LinkedList queue;
    
    public SelectingUnixStreamSource()
    throws IOException, UnknownHostException {
        this.link = new Link();
        this.queue = new LinkedList();
        this.file = new PosixFile(
            "/dev/log", 
            new FileAttributes("root", "root", FileMode.IRALL|FileMode.IWALL));
        this.reuseAddress = false;
	this.localHost = new RemoteInfo.Inet(InetAddress.getLocalHost());
        this.selector = null;
        this.serverSocket = null;
    }
    
    public Linkable getLinkable() {
        return link;
    }
    
    public void setFile(final PosixFile file) {
        this.file = file;
    }
    
    public void setKeepAlive(final boolean keepAlive) {
        this.keepAlive = keepAlive;
    }
    
    public void setMaxConnections(final int maxConnections) {
    }
    
    public void setReuseAddress(final boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }
    
    public void start() {
        Internal.warning(Internal.SOURCE_UNIX_STREAM, "Starting " + this);
        super.start();
        super.startAllThreads();
    }
    
    public void shouldStop() {
        super.shouldStop();
        closeSocket();
        synchronized (queue) {
            queue.notifyAll();
        }
    }
    
    public void run() {
        ensureSocket();
        processEvents();
        threadStopped();
    }
    
    protected synchronized void ensureSocket() {
        if (serverSocket != null) return;
        
        final Serie delays = new Serie.Bounded(60, new Serie.Exp(1, 2));
        while (!stopRequested()) {
            Throwable error;
            try {
                Internal.warning(Internal.SOURCE_UNIX_STREAM,
                                 "Opening server socket for " + this);
                openSocket();
                break;
            } catch (Throwable e) {
                error = e;
            }
            closeSocket();
            final double delay = delays.next();
            Internal.err(Internal.SOURCE_UNIX_STREAM, 
                         "Source " + this + " cannot listen!"
                         + " I will retry after " + delay + " seconds.",
                         error);
            try {
                ScojaThread.sleep(delay);
            } catch (InterruptedException e) {}
        }
        final SelectionKey ssKey
            = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        ssKey.attach(new AcceptTask(ssKey));
        addTask(new SelectTask());
    }

    protected void openSocket()
    throws IOException {    
        serverSocket = new UnixServerSocket();
        final UnixSocketAddress unixAddr = new UnixSocketAddress(file);
        unixAddr.clear();
        serverSocket.setKeepAlive(keepAlive);
        serverSocket.setReuseAddress(reuseAddress);
        serverSocket.bind(unixAddr);
        selector = Selector.open();
    }
    
    protected void closeSocket() {
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (Exception e) {}
            serverSocket = null;
        }
        if (selector != null) {
            try { selector.close(); } catch (Exception e) {}
            selector = null;
        }
    }

    protected void processEvents() {    
        while (!stopRequested()) {
            final Task task = nextTask();
            if (task != null) task.exec();
        }
    }
    
    public String toString() {
        return "Selecting Unix Stream source listening at " + file;
    }

    public String getName4Thread() {
        return "stream-traditional" + "@" + file;
    }
    
    
    //======================================================================
    protected void addTask(final Task task) {
        synchronized (queue) {
            queue.addLast(task);
            queue.notify();
        }
    }
    
    protected Task nextTask() {
        synchronized (queue) {
            try {
                while (queue.isEmpty() && !stopRequested()) queue.wait();
                if (stopRequested()) return null;
                return (Task)queue.removeFirst();
            } catch (InterruptedException e) {
                return null;
            }
        }
    }
    
    
    //======================================================================
    private interface Task {
        public void exec();
    }
    
    private class SelectTask implements Task {
        public void exec() {
            try {
                selector.select();
                final Set readyKeys = selector.selectedKeys();
                if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
                    Internal.debug(Internal.SOURCE_UNIX_STREAM,
                                   "Keys at " + SelectingUnixStreamSource.this 
                                   + " selector: " + selector.keys().size());
                }
                final Iterator rkit = readyKeys.iterator();
                while (rkit.hasNext()) {
                    final SelectionKey key = (SelectionKey)rkit.next();
                    key.interestOps(0);
                    final Task task = (Task)key.attachment();
                    if (task != null) {
                        queue.addLast(task);
                    } else {
                        Internal.crit(Internal.SOURCE_UNIX_STREAM,
                                      "No task for a selector");
                    }
                    rkit.remove();
                }
            } catch (Throwable e) {
                Internal.err(Internal.SOURCE_UNIX_STREAM,
                             "Error while selecting at " + this, e);
            }
            //It doesn't mind what happened, this select task must remain
            // in the task queue.
            queue.addLast(this);
        }
    }
    
    private class AcceptTask implements Task {
        protected final SelectionKey key;
        protected final UnixServerSocket server;
        
        public AcceptTask(final SelectionKey key) {
            this.key = key;
            this.server = (UnixServerSocket)key.channel();
        }
        
        public void exec() {
            try {
                final UnixSocket in = server.accept();
                final SelectionKey childKey
                    = in.register(selector, SelectionKey.OP_READ);
                childKey.attach(new ReadTask(childKey));
                key.interestOps(SelectionKey.OP_ACCEPT);
                selector.wakeup();
            } catch (Throwable e) {
                Internal.err(
                    Internal.SOURCE_UNIX_STREAM, "Source "
                    + SelectingUnixStreamSource.this
                    + " cannot accept a new connection!", e);
            }
        }
     }
    
    private class ReadTask implements Task {
        protected final SelectionKey key;
        protected final UnixSocket channel;
        protected final EventSplitter splitter;
        protected final byte[] buffer;
        
        public ReadTask(final SelectionKey key) {
            this.key = key;
            this.channel = (UnixSocket)key.channel();
            this.splitter = new EventSplitter(localHost, link);
            this.buffer = new byte[1024];
        }

        public void exec() {
            try {
                final InputStream in = channel.getInputStream();
                final int read = in.read(buffer);
                if (read == -1) {
                    close();
                } else {
                    splitter.add(buffer, 0, read);
                    key.interestOps(SelectionKey.OP_READ);
                    selector.wakeup();
                }
            } catch (IOException e) {
                close();
            } catch (Throwable e) {
                Internal.err(
                    Internal.SOURCE_UNIX_STREAM, "While processing data from"
                    + " a client of " + SelectingUnixStreamSource.this, e);
                close();
            }
        }
        
        private void close() {
            splitter.flush();
            key.cancel();
            key.attach(null);
            try { channel.close(); } catch (IOException e) {}
        }
    }
}
