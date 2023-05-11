
package org.scoja.server.core;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.scoja.server.source.Internal;

public abstract class ClusterSkeleton 
    implements Cluster, Runnable {

    public static final int DEFAULT_THREAD_COUNT = 2;

    protected final Set<ScojaThread> threads;
    protected boolean isRunning;
    protected volatile boolean stopRequested;
    protected int maxThreadCount;
    protected int currentThreadCount;
    
    protected long lastcpu;
    protected long lastuser;
    
    protected ClusterSkeleton() {
        this.threads = new HashSet<ScojaThread>();
        this.isRunning = false;
        this.stopRequested = false;
        this.maxThreadCount = DEFAULT_THREAD_COUNT;
        this.currentThreadCount = 0;
        this.lastuser = this.lastcpu = 0;
    }
    
    public synchronized boolean isRunning() {
        return isRunning;
    }
    
    public synchronized void start() {
        isRunning = true;
    }
    
    public synchronized void shouldStop() {
        stopRequested = true;
        for (final Thread t: threads) t.interrupt();
    }
    
    public synchronized boolean stopRequested() {
        return stopRequested;
    }
    
    public synchronized int getThreads() {
        return maxThreadCount;
    }
    
    public synchronized void setThreads(final int max) {
        maxThreadCount = max;
    }
    
    public synchronized int getCurrentThreads() {
        return currentThreadCount;
    }
    
    public synchronized void startAllThreads() {
        while (currentThreadCount < maxThreadCount) {
            startAnotherThread();
        }
    }
    
    public synchronized void startAnotherThread() {
        if (currentThreadCount == maxThreadCount) return;
        final ScojaThread newThread = new ScojaThread(this);
        currentThreadCount++;
        newThread.setName(getName4Thread() + "-" + currentThreadCount);
        threads.add(newThread);
        newThread.start();
        Internal.warning(Internal.THREAD, "Starting thread "
                         + currentThreadCount + " for " + this);
    }
    
    protected synchronized void threadStopped() {
        Internal.warning(Internal.THREAD, "Thread " + currentThreadCount
                         + " for " + this + " stopped");
        currentThreadCount--;
        threads.remove(Thread.currentThread());
    }
    
    public void stats(final Measure.Key key, final List<Measure> measures) {
        final ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
        if (!tmx.isThreadCpuTimeSupported()) return;
        long nowcpu = 0, nowuser = 0;
        synchronized (this) {
            for (final Thread t: threads) {
                final long id = t.getId();
                nowcpu += tmx.getThreadCpuTime(id);
                nowuser += tmx.getThreadUserTime(id);
            }
        }
        measures.add(new Measure(key, "cpu", nowcpu-lastcpu, nowcpu));
        measures.add(new Measure(key, "cpu-user", nowuser-lastuser, nowuser));
        lastcpu = nowcpu;
        lastuser = nowuser;
    }
    
    public abstract String getName4Thread();
}
