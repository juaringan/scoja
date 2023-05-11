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

import org.scoja.util.ExpiringLRUCache;
import org.scoja.server.core.Link;
import org.scoja.server.core.EventContext;

/**
 * Es un destino que manda siempre a un mismo fichero.
 * Satisface la funcionalidad básica de almacenamiento del syslogd
 * tradicional en Unix.
 * Pero para la funcionalidad de rotado por fecha de syslog-ng hay que
 * recurrir a {@link TemplateFileTarget}.
 * <p>
 * Lo más rápido sería tener el fichero destino permanentemente
 * abierto.
 * Pero lo vamos a extraer de un caché {@link FileLRUCache}, porque
 * es la única forma de conseguir que ningún destino de fichero pise
 * lo que otro pueda estar escribiendo (en una misma instancia de
 * Scoja).
 */
public class FixedFileTarget extends FileTarget {
    
    protected String filename;
    
    public FixedFileTarget(final FileSystem fileSystem,
                           final ExpiringLRUCache fileCache) {
        super(fileSystem, fileCache);
        this.filename = null;
    }
    
    public void setName(final String filename) {
        this.filename = filename;
    }
    
    protected String getFilename(final EventContext env) {
        return filename;
    }
}
