/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003  Mario Martínez
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

package org.scoja.server.target;

import org.scoja.util.LRUShell;
import org.scoja.util.ExpiringLRUCache;
import org.scoja.util.Graveyard;
import org.scoja.util.TransientMap;
import org.scoja.common.PriorityUtils;
import org.scoja.server.core.EventContext;
import org.scoja.server.source.Internal;

/**
 * Es una caché de ficheros abiertos.
 * Está implementada con un {@link ExpiringLRUCache}, así que hereda
 * de ella tanto sus capacidades (control por tamaño y tiempo) como su
 * idiosincrasia de uso (todos los accesos son a través del método
 * {@link #get(String,EventContext)}).
 * <p><b>Forma de uso</b>
 * Si <code>fileCache</code> contiene un FileLRUCache,
 * <code><pre>
 * final FileShell file = fileCache.get(filename, env);
   if (file != null) {
       try {
           file.ensureFileEntry([FileEntryBuilding]);
           final OutputStream out = file.getOutputStream();
           .....
           file.flush([FileFlushing]);
       } catch (IOException e) {
           System.err.println("UGGS: " + e.getMessage());
       } finally {
           file.release();
       }
   }
 * </pre></code>
 * El método {@link #get(String,EventContext)}
 * devuelve <code>null</code> si no 
 * pudo abrir el fichero; además produce un evento interno con level
 * {@link PriorityUtils#CRIT}; por tanto, no es necesario producir
 * ningún mensaje en el sitio de uso.
 * Para que un error de apertura no provoque una cantidad
 * desproporcianada de mensajes de error y de intentos de apertura (lo
 * que redundaría muy negativamente en el rendimiento de Scoja),
 * se mantiene una tabla con los ficheros que han fallado.
 */
public class FileLRUCache {

    public static final int DEFAULT_OPEN_MAX_SIZE = 500;
    public static final long DEFAULT_OPEN_MAX_INACTIVITY = 5*60*1000;
    public static final int DEFAULT_FAILED_MAX_SIZE = 50000;
    public static final long DEFAULT_FAILED_MAX_INACTIVITY = 5*60*1000;

    protected final ExpiringLRUCache openCache;
    protected final TransientMap failedCache;
    
    public FileLRUCache() {
        openCache = new ExpiringLRUCache(DEFAULT_OPEN_MAX_SIZE,
                                         DEFAULT_OPEN_MAX_INACTIVITY);
        openCache.setGraveyard(new Graveyard() {
                public void died(final Object key, final Object value) {
                    cachedDied(key, value);
                }
            });
        failedCache = new TransientMap(DEFAULT_FAILED_MAX_INACTIVITY);
        failedCache.setGraveyard(new Graveyard() {
                public void died(final Object key, final Object value) {
                    failedDied(key, value);
                }
            });
    }
    
    public void setSize(final int maxSize) {
        openCache.setSize(maxSize);
    }
    
    public void setInactivity(final long maxInactivity) {
        openCache.setInactivity(maxInactivity);
    }
    
    public void setFailedMemory(final long maxMemory) {
        failedCache.setFadingOut(maxMemory);
    }
    
    /**
     * Busca el fichero <code>filename</code> en esta caché.
     * Si no lo encuentra y recuerda haber fallado en esta operación 
     * hace poco, devuelve <code>null</code>.
     * Si no lo encuentra y no recuerda haber fallado en esta
     * operación hace poco, intenta abrirlo.
     * Si falla en la operación de abrirlo, genera un mensaje
     * {@link Internal#crit} y devuelve <code>null</code>.
     * En cualquier otro caso, el resultado es distinto de
     * <code>null</code> y ya contiene el fichero abierto.
     * <b>No se debe olvidar hacer un <code>release()</code> sobre el
     * resultado una vez que ya se ha terminado de usar.</b>
     */
    public FileShell get(final String filename,
                         final EventContext env) {
        final LRUShell shell = openCache.get(filename);
        FileShell file = (FileShell)shell.getValue();
        //Each execution path that keeps file == null should execute
        //shell.release().
        if (file == null) {
            final Object name = failedCache.get(filename);
            if (name == null) {
                file = new FileShell(filename, shell, this);
            } else {
                shell.release();
                if (PriorityUtils.INFO <= Internal.LOG_DETAIL) {
                    Internal.debug(env, Internal.TARGET_FILE,
                                   "Ignoring request to open file \""
                                   +filename+ "\" because recently failed");
                }
            }
        } else {
            if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
                Internal.debug(env, Internal.TARGET_FILE,
                               "File \"" +filename+ "\""
                               + " found in the open file cache");
            }
        }
        return file;
    }

    protected void cachedDied(final Object key, final Object value) {
        if (value != null) ((FileShell)value).expired();
    }
        
    protected void failedWhileOpening(final String filename) {
        failedCache.put(filename, filename);
    }
    
    protected void failedDied(final Object key, final Object value) {
        Internal.warning(Internal.TARGET_FILE,
                         "Forgetting that opening file \"" +key+ "\" failed."
                         + " I will try to open it at next request.");
    }
}
