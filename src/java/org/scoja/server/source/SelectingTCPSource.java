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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.scoja.common.PriorityUtils;
import org.scoja.trans.RemoteInfo;
import org.scoja.cc.io.QueuedOutputStream;
import org.scoja.server.core.ClusterSkeleton;
import org.scoja.server.core.DecoratedLink;
import org.scoja.server.core.ScojaThread;
import org.scoja.server.core.EventContext;
import org.scoja.server.core.Serie;
import org.scoja.server.core.Linkable;
import org.scoja.server.core.Link;
import org.scoja.server.core.Measurable;
import org.scoja.server.core.Measure;
import org.scoja.server.core.Event;
import org.scoja.server.parser.LightProtocolFactory;
import org.scoja.server.parser.ProtocolFactory;
import org.scoja.server.parser.StreamProtocol;
import org.scoja.server.source.Internal;

/**
 * @implementation
 * Un pequeño apunte con las dificultades que hemos encontrado con los Channel.
 * <ul>
 * <li>Para que un SocketChannel se pueda usar en un Select debe ponerse a
 *   no bloqueante. Pero entonces, no se puede leer del InputStream de
 *   su Socket asociado. No queda más remedio que usar la lectura con
 *   ByteBuffers.
 * <li>No es nada fácil hacer convivir un Selector con múltiples
 *   hebras.
 *   En los siguientes puntos repasaremos los problemas que hemos
 *   encontrado.
 * <li>Aunque se puede hacer, no sirve de nada tener varias hebras
 *   ejecutando un select() de un mismo Selector. En cuanto haya algo
 *   listo, todas las hebras pasarán con éxito por esta operación. Por
 *   supuesto, problemas de temporización pueden hacer que las últimas
 *   hebras no lleguen a pasar si antes las demás han consumido lo que
 *   ya estaba listo. Pero como veremos en los siguientes puntos, es muy
 *   difícil controlar esto.
 * <li>El conjunto que devuelve selectedKeys() es siempre el mismo.
 *   La ejecución de select() tiene como efecto meter elementos en
 *   este conjunto. El que selectedKeys() nos permita acceder al
 *   conjunto es para que tratemos los elementos que contiene y los
 *   vayamos borrando.
 *   La forma natural de recorrer los elementos es con un iterador.
 *   El que nos da este conjunto es fast-fail.
 *   Así que, si tenemos varias hebras haciendo select(), recorriendo
 *   y borrando en el iterador simultaneamente, lo más problable es que
 *   acabemos con un error de modificación concurrente.
 *   Hay que tener en cuenta que el select() mete elementos en el
 *   conjunto.
 *   Además, aunque parece que selectedKeys() devuelve siempre el
 *   mismo conjunto, no sé si es un comportamiento de implementación o
 *   de especificación (del javadoc parece deducirse que es lo segundo).
 *   Pero en todo caso es seguro que cada vez que pidamos un iterador
 *   a este objeto, nos dará uno nuevo; con lo que entre ellos se
 *   observarán errores de modificación concurrente.
 * <li>Cuando se pasa a tratar un <i>selectable</i>, no basta con
 *   borrarlo del conjunto de selectedKeys(). También hay que desactivar
 *   sus intereses con la operación interestOps(int). Si no se hace, en
 *   un contexto concurrente en donde la hebra H1 atiende al
 *   <i>selectable</i> S y la hebra H2 hace un select() antes de que H1
 *   haya completado su tarea, S volverá a estar en el conjunto de
 *   selectedKeys() y puede que un tercera hebra H3 se ponga a atenderlo
 *   simultaneamente con H1.
 * <li>Cuando se ha terminado de atender a un selectable S1, hay que
 *   reactivar sus intereses con interestOps(int).
 *   Pero además, hay que avisar al selector con un wakeup().
 *   Si no se hace lo primero, no se volverá a considerar a este objeto
 *   nunca más.
 *   Si no se hace lo segundo, este objeto no se volverá a considerar
 *   hasta que termine el select actual, es decir, hasta que cualquier
 *   otro selectable haga algo; si no hay ningún otro, o ningún otro tiene
 *   actividad, toda la de S1 se quedará esperando indefinidamente.
 * <li>El wakeup() no es gratis; saca a una hebra que esté esperando
 *   en el select(). Así que se ejecuta todo el bucle asociado al
 *   select() para nada. Pero no hay alternativa.
 * <li>
 * </ul>
 */
public class SelectingTCPSource 
    extends ClusterSkeleton
    implements Measurable, DecoratedLink, Runnable {
    
    public static final int DEFAULT_RECEIVE_BUFFER_SIZE = 0;
    public static final int DEFAULT_SEND_BUFFER_SIZE = 0;
    public static final int DEFAULT_IN_BUFFER_SIZE = 16*1024;
    public static final int DEFAULT_OUT_BUFFER_SIZE = 1024;
    
    protected final Link link;
    
    protected String ip;
    protected int port;
    protected boolean keepAlive;
    protected boolean reuseAddress;
    protected ProtocolFactory protocoler;
    protected int rcvsize;
    protected int sndsize;
    protected int insize;
    protected int outsize;

    protected ServerSocketChannel serverSocket;
    protected Selector selector;
    protected SelectionKey acceptKey;
    protected final LinkedList<SocketChannel> ins;
    protected final HashSet<SocketChannel> channels;
    protected final LinkedList<Task> tasks;
    
    public SelectingTCPSource() {
        this.link = new Link();
        this.ip = "127.0.0.1";
        this.port = 514;
        this.keepAlive = false;
        this.reuseAddress = true;
        this.protocoler = LightProtocolFactory.getInstance();
        this.rcvsize = DEFAULT_RECEIVE_BUFFER_SIZE;
        this.sndsize = DEFAULT_SEND_BUFFER_SIZE;
        this.insize = DEFAULT_IN_BUFFER_SIZE;
        this.outsize = DEFAULT_OUT_BUFFER_SIZE;
        this.serverSocket = null;
        this.selector = null;
        this.ins = new LinkedList<SocketChannel>();
        this.channels = new HashSet<SocketChannel>();
        this.tasks = new LinkedList<Task>();
    }
    
    public Linkable getLinkable() {
        return link;
    }
    
    public void setIp(final String ip) {
        this.ip = ip;
    }
    
    public void setPort(final int port) {
        this.port = port;
    }
    
    public void setKeepAlive(final boolean keepAlive) {
        this.keepAlive = keepAlive;
    }
    
    public void setMaxConnections(final int maxConnections) {
    }
    
    public void setReuseAddress(final boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }
    
    public void setProtocol(final ProtocolFactory protocoler) {
        this.protocoler = protocoler;
    }
    
    public void setReceiveBuffer(final int rcvsize) {
        this.rcvsize = rcvsize;
    }
    
    public void setSendBuffer(final int sndsize) {
        this.sndsize = sndsize;
    }
    
    public void setInBuffer(final int insize) {
        this.insize = insize;
    }
    
    public void setOutBuffer(final int outsize) {
        this.outsize = outsize;
    }
    
    public void start() {
        Internal.warning(Internal.SOURCE_TCP, "Starting " + this);
        super.start();
        super.startAllThreads();
    }
    
    public void shouldStop() {
        super.shouldStop();
        closeSocket();
        closeChannels();
        synchronized (tasks) {
            tasks.notifyAll();
        }
    }
    
    public void run() {
        ensureSocket();
        processEvents();
        closeChannels();
        threadStopped();
    }
    
    protected synchronized void ensureSocket() {
        if (serverSocket != null) return;
        
        final Serie delays = new Serie.Bounded(60, new Serie.Exp(1, 2));
        while (!stopRequested()) {
            Throwable error;
            try {
                Internal.warning(Internal.SOURCE_TCP,
                                 "Opening server socket for " + this);
                openSocket();
                break;
            } catch (Throwable e) {
                error = e;
            }
            closeSocket();
            final double delay = delays.next();
            Internal.err(Internal.SOURCE_TCP, 
                         "Source " + this + " cannot listen!"
                         + " I will retry after " + delay + " seconds.",
                         error);
            try {
                ScojaThread.sleep(delay);
            } catch (InterruptedException e) {}
        }
        try {
            acceptKey
                = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            acceptKey.attach(new AcceptTask());
        } catch (ClosedChannelException canBeIgnoredHere) {}
        addTask(new SelectTask());
    }
    
    protected void openSocket()
    throws IOException, UnknownHostException {
        serverSocket = ServerSocketChannel.open();
        serverSocket.configureBlocking(false);
        serverSocket.socket().setReuseAddress(reuseAddress);
        serverSocket.socket().bind(new InetSocketAddress(ip,port));
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
        //System.err.println("Process events: " + Thread.currentThread());
        while (!stopRequested()) {
            final Task task = nextTask();
            //System.err.println(Thread.currentThread() + " -> " + task);
            if (task != null) task.exec();
        }
    }
    
    public Measure.Key getMeasureKey() {
        return new Measure.Key(
            "source", "stream-" + protocoler.getName(), ip + ":" + port);
    }
    
    public void stats(final List<Measure> measures) {
        final Measure.Key key = getMeasureKey();
        super.stats(key, measures);
    }
    
    public String toString() {
        return "Selecting TCP source listening at " + ip + ":" + port;
    }

    public String getName4Thread() {
        return "stream-" + protocoler.getName() + "@" + ip + ":" + port;
    }

    
    //======================================================================
    protected void addTask(final Task task) {
        synchronized (tasks) {
            tasks.addLast(task);
            tasks.notify();
        }
    }

    protected Task nextTask() {
        synchronized (tasks) {
            try {
                while (tasks.isEmpty() && !stopRequested()) {
                    //System.err.println(Thread.currentThread() + ": waiting");
                    tasks.wait();
                    //System.err.println(Thread.currentThread() + ": awaked");
                }
                if (stopRequested()) return null;
                return tasks.removeFirst();
            } catch (InterruptedException e) {
                return null;
            }
        }
    }
    
    protected void addChannel(final SocketChannel in) {
        synchronized (ins) {
            ins.addLast(in);
        }
        synchronized (channels) {
            channels.add(in);
        }
    }
    
    protected void registerChannels()
    throws ClosedChannelException {
        synchronized (ins) {
            for (final SocketChannel in: ins) {
                //System.err.println("Registering");
                final SelectionKey childKey = in.register(selector, 0);
                //System.err.println("Registered");
                childKey.attach(new IOTask(childKey));
            }
            ins.clear();
        }
    }
    
    protected void closeChannels() {
        synchronized (channels) {
            for (Iterator it = channels.iterator(); it.hasNext(); ) {
                final SocketChannel chn = (SocketChannel)it.next();
                try { chn.close(); } catch (IOException e) {}
            }
            channels.clear();
        }
    }
        
    //======================================================================
    private interface Task {
        public void exec();
    }
    
    private class SelectTask implements Task {
        public void exec() {
            try {
                registerChannels();
                selector.select();
                final Set readyKeys = selector.selectedKeys();
                if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
                    Internal.debug(Internal.SOURCE_TCP,
                                   "Keys at " + SelectingTCPSource.this 
                                   + " selector: " + selector.keys().size());
                }
                final Iterator rkit = readyKeys.iterator();
                while (rkit.hasNext()) {
                    final SelectionKey key = (SelectionKey)rkit.next();
                    key.interestOps(0);
                    final Task task = (Task)key.attachment();
                    if (task != null) {
                        addTask(task);
                    } else {
                        Internal.crit(Internal.SOURCE_TCP,
                                      "No task for a selector");
                    }
                    rkit.remove();
                }
            } catch (Throwable e) {
                Internal.err(Internal.SOURCE_TCP,
                             "Error while selecting at " + this, e);
            }
            //It doesn't mind what happened, this select task must remain
            // in the task queue.
            addTask(this);
        }
    }
    
    private class AcceptTask implements Task {
        public void exec() {
            SocketChannel in = null;
            try {
                in = serverSocket.accept();
                in.configureBlocking(false);
                final Socket ins = in.socket();
                ins.setKeepAlive(keepAlive);
                if (rcvsize > 0) ins.setReceiveBufferSize(rcvsize);
                if (sndsize > 0) ins.setSendBufferSize(sndsize);
                addChannel(in);
                acceptKey.interestOps(SelectionKey.OP_ACCEPT);
                selector.wakeup();
            } catch (Throwable e) {
                if (in != null) try { in.close(); } catch (IOException e2) {}
                Internal.err(
                    Internal.SOURCE_TCP, "Source " + SelectingTCPSource.this
                    + " cannot accept a new connection!", e);
            }
        }
     }
    
    private class IOTask implements Task {
        protected final SelectionKey key;
        protected final SocketChannel channel;
        protected final int myoutsize;
        protected final QueuedOutputStream outBuffer;
        protected final StreamProtocol protocol;
        
        public IOTask(final SelectionKey key) {
            this.key = key;
            this.channel = (SocketChannel)key.channel();
            this.myoutsize = outsize;
            this.outBuffer = new QueuedOutputStream(2*this.myoutsize);
            this.protocol = protocoler.newStreamProtocol(
                RemoteInfo.Inet.from(channel), link, outBuffer);
            this.protocol.setInBuffer(insize);
            determineInterests();
        }

        public void exec() {
            try {
                sendOutput();
                processInput();
                sendOutput();
                if (!channel.isOpen()) {
                    close();
                } else {
                    determineInterests();
                    selector.wakeup();
                }
            } catch (Throwable e) {
                //e.printStackTrace(System.err);
                Internal.err(
                    Internal.SOURCE_TCP, "While processing data from a client"
                    + "(" + channel.socket().getRemoteSocketAddress() + ")"
                    + " of " + SelectingTCPSource.this, e);
                close();
            }
        }
        
        private void determineInterests() {
            int ops = 0;
            if (outBuffer.available() > 0) ops = SelectionKey.OP_WRITE;
            if (outBuffer.available() <= myoutsize) ops |=SelectionKey.OP_READ;
            key.interestOps(ops);
        }
        
        private void sendOutput()
        throws IOException {
            if (outBuffer.available() > 0 && key.isWritable()) {
                outBuffer.writeTo(channel);
            }
        }
        
        private void processInput()
        throws IOException {
            if (outBuffer.available() <= myoutsize && key.isReadable()) {
                protocol.processAvailable(channel);
            }
        }
        
        private void close() {
            try { protocol.flush(); } catch (Throwable e) {}
            key.attach(null);
            try { channel.close(); } catch (IOException e) {}
            synchronized (channels) { channels.remove(channel); }
        }
    }
}
