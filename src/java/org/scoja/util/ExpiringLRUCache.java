
package org.scoja.util;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;

/**
 * Es una tabla con un tamaño limitado en espacio y en tiempo.
 *
 * <p>
 * Está pensada para usarse en un contexto concurrente, en donde
 * muchas hebras comparten los recursos de esta caché.
 * Se supone que (1) los objetos de la caché tienen un coste de
 * construcción y que, por tanto, hay que evitar que dos hebras
 * construyan el mismo porque ambas lo vieron a la vez ausente.
 * Además, también se supone que (2) los objetos deben liberar un recurso
 * al final de su vida en la caché.
 * La caché avisa de su muerte mediante
 * {@link Graveyard#died}.
 * Curiosamente, estas dos suposiciones tienen una relación: cuando
 * dos hebras construyen un mismo objeto, sólo uno de ellos termina
 * en la caché, y el otro, si no se tiene buen cuidado, termina
 * perdido, bloqueando su recurso hasta que se recoja de la basura.
 *
 * <p>
 * La limitación en tiempo quiere decir que la caché mata todos los
 * objetos que lleven más de un cierto tiempo sin haberse usado.
 *
 * <p>
 * La limitación en espacio tiene dos variantes.
 * En la <i>laxa</i>, se admite que se sobrepase el máximo si son
 * necesarios simultaneamente; esta interpretación no tiene problemas
 * de interbloqueo.
 * En la <i>estricta</i>, se deja detenidas a las peticiones que
 * intenten sobrepasar el límite; esta interpretación tiene problemas
 * de interbloqueo si las hebras piden varios recursos
 * simultaneamente.
 * Para nuestro uso en Scoja, ambas semánticas son adecuadas, porque
 * cada hebra sólo pide un recurso: el fichero donde tiene que
 * escribir.
 * Por ahora sólo se implementa la <i>laxa</i> porque es más
 * sencilla.
 *
 * <p><b>Forma de uso</b>
 * La forma de uso de esta caché es un tanto atípica.
 * Para acceder a un elemento hay que utilizar el método
 * {@link #get(Object)}; pero aquí se acaban las similitudes con una
 * tabla.
 * Primero el resultado de este método no es el valor que estamos
 * buscando, sino un {@link LRUShell} que nos permitirá llegar a ese
 * valor.
 * Segundo, hagamos lo que hagamos con el <code>LRUShell</code>,
 * cuando hayamos terminado de usarlo debemos indicarlo llamando a su
 * método {@link LRUShell#release()}.
 * Cuando se accede a un elemento con {@link #get(Object)}, su caché
 * anota que se está usando e impide su destrucción, por mucho que se
 * llene la caché, o por mucho tiempo que pase.
 * Cuando se llama a {@link LRUShell#release()}, la caché
 * entiende que ya no se está usando y lo empieza a tratar como un
 * objeto que puede expirar.
 * Por tanto, si olvidamos hacer esta llamada, terminaremos
 * abarrotando la memoria con una caché llena de objetos que nunca van
 * a morir.
 * <p>
 * El método {@link #get(Object)} devuelve un {@link LRUShell} incluso
 * para las claves que nunca se han definido.
 * Por supuesto este <code>LRUShell</code> no tiene ningún valor
 * asociado y su método {@link LRUShell#getValue()} devolverá
 * <code>null</code>.
 * Pero el comportamiento de {@link LRUShell#getValue()} es bastante
 * más complejo; si varias hebras acceden a la vez a un mismo
 * <code>LRUCache</code> sin valor, solo la primera que ejecute
 * <code>getValue()</code> verá un <code>null</code>; el resto de
 * hebras se quedarán detenidas hasta que, o bien esa hebra da un
 * valor con {@link LRUShell#put(Object)}, o indica que ya lo ha
 * terminado de usar con {@link LRUShell#release()}.
 * <p>
 * Cuando un objeto ya tiene valor, cualquier hebra puede acceder a su
 * {@link LRUShell} y a su valor asociado, sin tener que esperar a
 * otra. Es decir, esta caché no serializa el acceso a sus valores.
 * Un valor puede expirar cuando ninguna hebra lo está usando, es
 * decir, cuando todas la hebras que lo consiguieron con
 * <code>get()</code> ya han ejecutado un <code>release()</code>.
 * <p>
 * <code><pre>
 * LRUShell shell = null;
 * try {
 *     shell = cache.get(key);
 *     if (shell.getValue() == null) {
 *         shell.put(new KillMe(value));
 *     }
 * } finally {
 *     if (shell != null) shell.release();
 * }
 * </pre></code>
 */
public class ExpiringLRUCache {

    protected final Shell lruQueue;
    protected final Map map;

    protected boolean closed;    
    protected int maxSize;
    protected long maxInactivity;
    protected Graveyard graveyard;
    
    public ExpiringLRUCache(final int maxSize,
                            final long maxInactivity) {
        this.lruQueue = new Shell();
        this.map = new HashMap();
        this.closed = false;
        this.maxSize = maxSize;
        this.maxInactivity = maxInactivity;
        this.graveyard = null;
    }
    
    public void setSize(final int maxSize) {
        this.maxSize = maxSize;
    }
    
    public void setInactivity(final long maxInactivity) {
        this.maxInactivity = maxInactivity;
    }
    
    public void setGraveyard(final Graveyard graveyard) {
        this.graveyard = graveyard;
    }
    
    public synchronized LRUShell get(final Object key) {
        if (closed) throw new IllegalStateException("Closed");
        Shell shell = (Shell)map.get(key);
        if (shell == null) {
            shell = new Shell(key);
            shell.linkAfter(lruQueue);
            shell.addUsage();
            map.put(key, shell);
            killOldest(1);
        } else {
            shell.unlink();
            shell.linkAfter(lruQueue);
            shell.addUsage();
            killOldest(0);
        }
        return shell;
    }
    
    protected synchronized void release(final Shell shell) {
        shell.removeUsage();
        if (shell.getUsage() == 0
            && (shell.getCurrentValue() == null || closed)) {
            kill(shell);
        } else {
            shell.unlink();
            shell.linkAfter(lruQueue);
            killOldest(0);
        }
    }
    
    protected void killOldest(final int free) {
        final long killtime = System.currentTimeMillis() - maxInactivity;
        int toKill = map.size() - free - maxSize;
        Shell last = lruQueue.getPrevious();
        while (last != lruQueue
               && (last.getLastUsageTime() <= killtime
                   || toKill > 0)) {
            final Shell next = last.getPrevious();
            if (last.getUsage() == 0) {
                kill(last);
                toKill--;
            }
            last = next;
        }
    }
    
    protected void kill(final Shell shell) {
        if (graveyard != null) {
            graveyard.died(shell.getKey(), shell.getCurrentValue());
        }
        shell.unlink();
        map.remove(shell.getKey());
    }
    
    public synchronized void killAll() {
        Shell last = lruQueue.getPrevious();
        while (last != lruQueue) {
            final Shell next = last.getPrevious();
            if (last.getUsage() == 0) kill(last);
            last = next;
        }
    }
    
    public synchronized void close() {
        closed = true;
        killAll();
    }

    
    //======================================================================
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        final Shell first = lruQueue.getNext();
        while (first != lruQueue) {
            sb.append(first).append('\n');
        }
        return sb.toString();
    }
    
    
    //======================================================================
    private class Shell implements LRUShell {
    
        protected final Object key;
        protected Object value;
        protected boolean computing;
        
        protected int usages;
        protected long lastUsage;
        
        protected Shell prev;
        protected Shell next;
        
        public Shell() {
            this(null);
        }
        
        public Shell(final Object key) {
            this.key = key;
            this.value = null;
            this.computing = false;
            
            this.usages = 0;
            this.lastUsage = Long.MAX_VALUE;
            
            this.prev = this.next = this;
        }

        protected int getUsage() {
            return usages;
        }
        
        protected long getLastUsageTime() {
            return lastUsage;
        }
                
        protected void addUsage() {
            this.usages++;
            this.lastUsage = System.currentTimeMillis();
        }
        
        protected void removeUsage() {
            this.usages--;
            this.lastUsage = System.currentTimeMillis();
        }
    
        protected Shell getPrevious() {
            return prev;
        }
        
        protected Shell getNext() {
            return next;
        }
        
        protected void unlink() {
            next.prev = prev;
            prev.next = next;
        }
        
        protected void linkAfter(final Shell befo) {
            prev = befo;
            next = befo.next;
            this.next.prev = this;
            this.prev.next = this;
        }
        
        protected Object getCurrentValue() {
            return value;
        }
    
        public Object getKey() {
            return key;
        }
        
        public synchronized Object getValue() {
            while (value == null && computing) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
            if (value == null) computing = true;
            return value;
        }
        
        public synchronized void put(final Object value)
            throws IllegalStateException {
            if (this.value != null) {
                throw new IllegalStateException
                    ("Trying to change value for " + key);
            }
            this.value = value;
            computing = false;
            notifyAll();
        }
        
        public synchronized void release() {
            computing = false;
            ExpiringLRUCache.this.release(this);
            notifyAll();
        }
        
        
        //============================================================
        public String toString() {
            return "Key: " + key
                + ", value: " + value
                + ", computing: " + computing
                + ", usages: " + usages
                + ", last usage: " + new Date(lastUsage);
        }
    }
}
