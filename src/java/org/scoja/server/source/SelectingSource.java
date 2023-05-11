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

import org.scoja.common.PriorityUtils;
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

import org.scoja.trans.Transport;
import org.scoja.trans.TransportService;
import org.scoja.trans.TransportLine;
import org.scoja.trans.ConnectState;
import org.scoja.trans.SelectionHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.net.Socket;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

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
public class SelectingSource 
    extends ClusterSkeleton
    implements Measurable, DecoratedLink, Runnable {
    
    public static final int DEFAULT_RECEIVE_BUFFER_SIZE = 0;
    public static final int DEFAULT_SEND_BUFFER_SIZE = 0;
    public static final int DEFAULT_IN_BUFFER_SIZE = 16*1024;
    public static final int DEFAULT_OUT_BUFFER_SIZE = 1024;
    
    protected final Link link;

    protected Transport<?> transport;
    protected ProtocolFactory protocoler;
    protected int insize;
    protected int outsize;

    protected TransportService service;
    protected Selector selector;
    protected SelectionHandler acceptHandler;
    protected final LinkedList<TransportLine> newLines;
    protected final HashSet<TransportLine> lines;
    protected final LinkedList<Task> tasks;
    
    public SelectingSource() {
        this.link = new Link();
        this.transport = null;
        this.protocoler = LightProtocolFactory.getInstance();
        this.insize = DEFAULT_IN_BUFFER_SIZE;
        this.outsize = DEFAULT_OUT_BUFFER_SIZE;
        this.service = null;
        this.selector = null;
        this.newLines = new LinkedList<TransportLine>();
        this.lines = new HashSet<TransportLine>();
        this.tasks = new LinkedList<Task>();
    }
    
    public Linkable getLinkable() {
        return link;
    }
    
    public void setTransport(final Transport transport) {
        this.transport = transport;
    }
    
    public void setMaxConnections(final int maxConnections) {
    }
    
    public void setProtocol(final ProtocolFactory protocoler) {
        this.protocoler = protocoler;
    }
    
    public void setInBuffer(final int insize) {
        this.insize = insize;
    }
    
    public void setOutBuffer(final int outsize) {
        this.outsize = outsize;
    }
    
    public void start() {
        Internal.warning(Internal.SOURCE_TRANS, "Starting " + this);
        super.start();
        super.startAllThreads();
    }
    
    public void shouldStop() {
        super.shouldStop();
        closeService();
        closeLines();
        synchronized (tasks) {
            tasks.notifyAll();
        }
    }
    
    public void run() {
        ensureService();
        processEvents();
        closeLines();
        threadStopped();
    }
    
    protected synchronized void ensureService() {
        if (service != null) return;
        
        final Serie delays = new Serie.Bounded(60, new Serie.Exp(1, 2));
        while (!stopRequested()) {
            Throwable error;
            try {
                Internal.warning(Internal.SOURCE_TRANS,
                                 "Opening server socket for " + this);
                openService();
                break;
            } catch (Throwable e) {
                error = e;
            }
            closeService();
            final double delay = delays.next();
            Internal.err(Internal.SOURCE_TRANS, 
                         "Source " + this + " cannot listen!"
                         + " I will retry after " + delay + " seconds.",
                         error);
            try {
                ScojaThread.sleep(delay);
            } catch (InterruptedException e) {}
        }
        addTask(new SelectTask());
    }
    
    protected void openService()
    throws IOException {
        service = transport.server();
        service.bind();
        selector = Selector.open();
        acceptHandler = service.register(
            new SelectionHandler(selector, new AcceptTask()));
    }

    protected void closeService() {
        if (service != null) {
            try { service.close(); } catch (Throwable e) {}
            service = null;
        }
        if (selector != null) {
            try { selector.close(); } catch (Throwable e) {}
            selector = null;
        }
        acceptHandler = null;
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
            "source", protocoler.getName() + "-" + transport.layers(),
            transport.endPointId());
    }
    
    public void stats(final List<Measure> measures) {
        final Measure.Key key = getMeasureKey();
        super.stats(key, measures);
    }
    
    public String toString() {
        return "Selecting source listening at " + transport;
    }

    public String getName4Thread() {
        return protocoler.getName() + "-" + transport.layers()
            + "@" + transport.endPointId();
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
    
    protected void addLine(final TransportLine line) {
        synchronized (newLines) { newLines.addLast(line); }
        synchronized (lines) { lines.add(line); }
    }
    
    protected void registerNewLines()
    throws ClosedChannelException {
        synchronized (newLines) {
            for (final TransportLine line: newLines) {
                final SelectionHandler handler = line.register(
                    new SelectionHandler(selector, null));
                handler.attach(new IOTask(line, handler));
            }
            newLines.clear();
        }
    }
    
    protected void closeLines() {
        synchronized (lines) {
            for (final TransportLine line: lines) {
                try { line.close(); } catch (IOException ignored) {}
            }
            lines.clear();
        }
    }
        
    //======================================================================
    private interface Task {
        public void exec();
    }
    
    private class SelectTask implements Task {
        public void exec() {
            try {
                if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
                    final int newLinesCnt = newLines.size();
                    final int linesCnt = lines.size();
                    final int tasksCnt = tasks.size();
                    Internal.debug(Internal.SOURCE_TRANS,
                            "Exec/starting"
                            + "; thread: " + Thread.currentThread()
                            + ", tasks: " + tasksCnt
                            + ", lines: " + linesCnt
                            + ", new lines: " + newLinesCnt
                            + ", keys: " + selector.keys().size());
                }
                registerNewLines();
                selector.select();
                final Set<SelectionKey> readyKeys = selector.selectedKeys();
                if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
                    final int newLinesCnt = newLines.size();
                    final int linesCnt = lines.size();
                    final int tasksCnt = tasks.size();
                    Internal.debug(Internal.SOURCE_TRANS,
                            "Exec/selected"
                            + "; thread: " + Thread.currentThread()
                            + ", tasks: " + tasksCnt
                            + ", lines: " + linesCnt
                            + ", new lines: " + newLinesCnt
                            + ", keys: " + selector.keys().size()
                            + ", selected keys: " + readyKeys.size());
                }
                for (final Iterator<SelectionKey> rkit = readyKeys.iterator();
                     rkit.hasNext(); ) {
                    final SelectionHandler key
                        = (SelectionHandler)rkit.next().attachment();
                    key.disable();
                    final Task task = (Task)key.attachment();
                    if (task != null) {
                        addTask(task);
                    } else {
                        Internal.crit(Internal.SOURCE_TRANS,
                                      "No task for a selector");
                    }
                    rkit.remove();
                }
            } catch (Throwable e) {
                Internal.err(Internal.SOURCE_TRANS,
                             "Error while selecting at " + this, e);
            }
            //It doesn't mind what happened, this select task must remain
            // in the task queue.
            addTask(this);
        }
    }
    
    private class AcceptTask implements Task {
        public void exec() {
            TransportLine line = null;
            try {
                line = service.accept();
                addLine(line);
                acceptHandler.enable();
                selector.wakeup();
            } catch (Throwable e) {
                if (line != null)
                    try { line.close(); } catch (IOException e2) {}
                Internal.err(
                    Internal.SOURCE_TRANS, "Source " + SelectingSource.this
                    + " cannot accept a new connection!", e);
            }
        }
     }
    
    private class IOTask implements Task {
        protected final TransportLine line;
        protected final SelectionHandler key;
        protected final int myoutsize;
        protected final QueuedOutputStream outBuffer;
        protected final StreamProtocol protocol;
        
        public IOTask(final TransportLine line, final SelectionHandler key) {
            this.line = line;
            this.key = key;
            this.myoutsize = outsize;
            this.outBuffer = new QueuedOutputStream(2*this.myoutsize);
            this.protocol = protocoler.newStreamProtocol(
                line.remote(), link, outBuffer);
            this.protocol.setInBuffer(insize);
            determineInterests();
        }

        public void exec() {
            try {
                sendOutput();
                processInput();
                sendOutput();
                if (!line.connectState().open()) {
                    close();
                } else {
                    determineInterests();
                    selector.wakeup();
                }
            } catch (Throwable e) {
                //e.printStackTrace(System.err);
                Internal.err(
                    Internal.SOURCE_TRANS, 
                    "While processing data from a client"
                    + "(" + line.remote().inetAddress() + ")"
                    + " of " + SelectingSource.this, e);
                close();
            }
        }
        
        private void determineInterests() {
            int ops = 0;
            if (outBuffer.available() > 0) ops = SelectionKey.OP_WRITE;
            if (outBuffer.available() <= myoutsize) ops |=SelectionKey.OP_READ;
            key.interestOps(ops);
            key.enable();
        }
        
        private void sendOutput()
        throws IOException {
            if (outBuffer.available() > 0 /*&& key.isWritable()*/) {
                outBuffer.writeTo(line.channel());
            }
        }
        
        private void processInput()
        throws IOException {
            if (outBuffer.available() <= myoutsize /*&& key.isReadable()*/) {
                //This executes line.channel().close() when a -1 is seen in
                // the input channel.
                protocol.processAvailable(line.channel());
            }
        }
        
        private void close() {
            if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
                final int newLinesCnt = newLines.size();
                final int linesCnt = lines.size();
                final int tasksCnt = tasks.size();
                Internal.debug(Internal.SOURCE_TRANS,
                        "IO/close"
                        + "; thread: " + Thread.currentThread()
                        + ", tasks: " + tasksCnt
                        + ", lines: " + linesCnt
                        + ", new lines: " + newLinesCnt
                        + ", line: " + line
                        + ", line state: " + line.connectState());
            }
            try { protocol.flush(); } catch (Throwable e) {}
            //key.attach(null);
            try { line.close(); } catch (Throwable e) {}
            synchronized (lines) { lines.remove(line); }
        }
    }
}
