
package org.scoja.server.core;

/**
 * Todos los elementos activos (que tienen sus propias hebras) deben
 * implementar esta interfaz para que se los pueda trazar.
 */
public interface Cluster {

    public boolean isRunning();
    
    public void start();
    
    public void shouldStop();
    
    public int getThreads();
    
    public void setThreads(final int max);
    
    public int getCurrentThreads();
}
