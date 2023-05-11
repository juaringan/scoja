
package org.scoja.server.core;

public class IterativeCluster extends ClusterSkeleton {

    protected final IterativeRutine rutine;
    
    public IterativeCluster(final IterativeRutine rutine) {
        super();
        this.rutine = rutine;
    }
    
    public void run() {
        for (;;) {
            synchronized (this) {
                if (!shouldKeepAlive()) {
                    threadStopped();
                    break;
                }
            }
            executeStep();
        }
    }
    
    public boolean shouldKeepAlive() {
        return !stopRequested() 
            && (getCurrentThreads() <= getThreads());
    }
    
    public void executeStep() {
        rutine.executeStep();
    }
    
    public String getName4Thread() {
        return "unknown";
    }
} 
