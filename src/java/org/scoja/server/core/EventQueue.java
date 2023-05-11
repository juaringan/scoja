
package org.scoja.server.core;

import java.util.LinkedList;

/**
 * Estas colas desacoplan la captura de eventos de su procesamiento
 * posterior a través de enlaces, filtros y destinos.
 * <p>
 * Puede haber tantas instancias como se desee.
 * <p>
 * Estas colas son activas: se encargan de ir procesando los eventos
 * que le llegan.
 * Se puede configurar cuántas hebras realizan esta labor.
 * En el constructor se indica el máximo número de hebras que queremos
 * (por defecto {@link #DEFAULT_THREAD_COUNT}),
 * pero esta cantidad se puede cambiar dinámicamente con
 * {@link #setThreads(int)}.
 * Cuando sobran hebras, se destruyen al terminar el proceso del
 * evento con el que estén.
 * Sin embargo, las hebras sólo se crean cuando llega un nuevo evento,
 * todas las hebras están ocupadas y todavía no se ha llegado al
 * máximo de hebras.
 * Por tanto, el número real de hebras puede no llegar al máximo;
 * aunque es muy probable porque crece monótonamente.
 *
 * <p><b>Waiting for all pending events to be completely processed</b>
 * This is not a easy problem, because many threads can be processing events
 * asynchronously.
 * Being an exact solution requires non-trivial data structures:
 * a waiting thread should be registered on currently pending events
 * (let be N events);
 * when an event is processed, the waiting threads are notified;
 * a thread waits until it receives N notifications.
 * Pending events include those in {@link #queue} but also those being
 * currenlty processed by some thread.
 * <p>
 * We have implemented an approximate solution.
 * A sequential id is implicitly given to each event received at
 * {@link #process}.
 * This sequence starts with 1; it is traced with {@link #recvcnt}.
 * When an event is removed from the queue its id is <i>recovered</i>
 * and remembered untif fully processed;
 * then {@link #donecnt} (how many events has been fully processed)
 * and {@link #donemax} (the greater id of all fully processed events)
 * are updated.
 * When a wait is requested, the current thread will wait (at least) until
 * all current events are processed.
 * To know when this happens an approximation is used.
 * The last pending id is remembered (say E).
 * First, if {@link #donecnt} == {@link #donemax}, there is no processing
 * holes in the pending event sequence.
 * Second, if, additionally, {@link #donecnt} &gt;= E, all the events has been 
 * processed.
 * <p>
 * Synchronization is a problem and is resolved also approximately.
 * <tt>this</tt> is reserved for the queue processing;
 *  any instance variable modification is done with this monitor locked.
 * {@link currentLock} is introduced only for waiting.
 * The possible lock sequences are:
 *   (1) this,
 *   (2) currentLock, this.
 * So waiting on currentLock is done when this is not locked,
 * and a possible interleaving can call currentLock.notifyAll() before the
 * corresponding wait() is executed.
 * To solve this, a timedout wait is done.
 * <p>
 * So, ti
 */
public class EventQueue extends ClusterSkeleton {

    protected final String name;
    protected final LinkedList queue;
    protected int threadsAwaiting;
    
    protected final Object currentLock;
    protected int waitingCurrent;
    protected long recvcnt;
    protected long done;
    protected long donecnt;
    protected long donemax;
    
    public EventQueue(final String name) {
        super();
        this.name = name;
        this.queue = new LinkedList();
        this.threadsAwaiting = 0;
        this.currentLock = new Object();
        this.waitingCurrent = 0;
        this.recvcnt = this.done = this.donecnt = this.donemax = 0;
    }

    public void process(final EventContext eenv) {
        if (eenv == null) return;
        synchronized (this) {
            if (isRunning && !stopRequested) {
                recvcnt++;
                queue.addLast(eenv);
                if (threadsAwaiting == 0) startAnotherThread();
                this.notify();
                return;
            }
        }
        eenv.process();
    }
    
    public void run() {
        while (!stopRequested) {
            EventContext context = null;
            long current = 0;
            synchronized (this) {
                if (queue.isEmpty()) {
                    threadsAwaiting++;
                    try {
                        this.wait();
                    } catch (InterruptedException e) {}
                    threadsAwaiting--;
                } else {
                    context = (EventContext)queue.removeFirst();
                    current = recvcnt - queue.size();
                }
            }
            if (context != null) {
                try {
                    context.process();
                } finally {
                    done(current);
                }
            }
        }
        synchronized (this) {
            while (!queue.isEmpty()) {
                ((EventContext)queue.removeFirst()).process();
            }
        }
    }
    
    public void waitCurrent()
    throws InterruptedException {
        final long current;
        synchronized (this) { current = recvcnt; waitingCurrent++; }
        try {
            synchronized (currentLock) {
                for (;;) {
                    final boolean shouldWait;
                    synchronized (this) {
                        shouldWait = current > done;
                    }
                    if (!shouldWait) break;
                    currentLock.wait(1000);
                }
            }
        } finally {
            synchronized (this) { waitingCurrent--; }
        }
    }

    protected void done(final long current) {    
        final boolean shouldNotify;
        synchronized (this) {
            donecnt++;
            donemax = Math.max(donemax, current);
            if (donecnt == donemax) done = donecnt;
            shouldNotify = waitingCurrent > 0;
        }
        if (shouldNotify) synchronized (currentLock) {
            currentLock.notifyAll();
        }
    }
    
    public String toString() {
        return "Queue \"" + name + "\"" + " with"
            + " " + queue.size() + " queued events"
            + ", " + getThreads() + " max threads"
            + ", " + getCurrentThreads() + " threads started"
            + ", " + threadsAwaiting + " threads awaiting";
    }
    
    public String getName4Thread() {
        return "queue-" + name;
    }
}
