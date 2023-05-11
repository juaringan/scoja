
package org.scoja.util;

/**
 * Es un anexo a la interfaz de {@link ExpiringLRUCache}.
 * Estos m�todos complementan a {@link ExpiringLRUCache#get(Object)}
 * para conseguir una interfaz completa de tabla.
 * No obstante, la sem�ntica de acceso concurrente para las claves sin
 * valor es m�s compleja que para las tablas normales; puede
 * estudiarse en la secci�n <b>Forma de uso</b> de
 * {@link ExpiringLRUCache}.
 */
public interface LRUShell {

    /**
     * Devuelve la clave a la que est� asociado este objeto.
     * Generalmente es un dato redundante, porque se tuvo que conocer
     * para poder buscar este objeto en una cache.
     */
    public Object getKey();

    /**
     * Devuelve el valor que contiene este objeto y que se supone que
     * es el valor para la clave {@link #getKey()}.
     * Cuando este objeto no tiene valor asociado, la primera llamada
     * devuelve <code>null</code> y las siguiente se quedan bloqueadas
     * hasta que la primera llama a {@link #put(ExpiringObject)} o
     * {@link #release()}.
     */
    public Object getValue();
    
    /**
     * Pone el valor a <code>value</code>, cuando el objeto est�
     * indefinido.
     * Pero produce una excepci�n {@link IllegalStateException} si ya
     * hab�a recibido un valor.
     */
    public void put(Object value) throws IllegalStateException;
    
    /**
     * Indica a la cach� de la que lo hemos sacado que ya no lo
     * estamos usando y que, por nosotros, puede empezar a pensar en
     * su muerte.
     */
    public void release();
}
