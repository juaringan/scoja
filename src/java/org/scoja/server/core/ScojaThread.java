
package org.scoja.server.core;

public class ScojaThread extends Thread {

    private static ThreadLocal threadECtx = new ThreadLocal() {
            protected Object initialValue() {
                return null;
            }
        };
    
    public static EventContext setCurrentEventContext(final EventContext ectx){
        final Thread thread = Thread.currentThread();
        if (thread instanceof ScojaThread) {
            return ((ScojaThread)thread).setEventContext(ectx);
        } else {
            final EventContext previous = (EventContext)threadECtx.get();
            threadECtx.set(ectx);
            return previous;
        }
    }
    
    public static EventContext getCurrentEventContext() {
        final Thread thread = Thread.currentThread();
        if (thread instanceof ScojaThread) {
            return ((ScojaThread)thread).getEventContext();
        } else {
            return (EventContext)threadECtx.get();
        }
    }

    
    //======================================================================
    public static void sleep(final double seconds)
    throws InterruptedException {
        final long millis = (long)(1000 * seconds);
        Thread.sleep(millis);
    }

    //======================================================================
    protected EventContext ectx;

    public ScojaThread() {
        super();
        this.ectx = null;
    }
    
    public ScojaThread(final Runnable toRun) {
        super(toRun);
        this.ectx = null;
    }
    
    public EventContext setEventContext(final EventContext ectx) {
        final EventContext previous = this.ectx;
        this.ectx = ectx;
        return previous;
    }
    
    public EventContext getEventContext() {
        return this.ectx;
    }
}
