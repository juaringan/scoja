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
import org.scoja.server.template.Template;

/**
 * Es el destino básico de Scoja; manda a un fichero cuyo nombre puede
 * depender de la fecha del evento y de muchos otros datos como el
 * nombre de la máquina origen, etc.
 * El nombre del fichero se define con un {@link Template}.
 *
 * <p><b>Caché de ficheros</b>
 * Para cada evento a procesar, hay que calcular el nombre del fichero
 * en donde hay que escribir.
 * Este cálculo no es muy costoso, pero abrir y cerrar el fichero es
 * extraordinariamente lento.
 * Hicimos unas pruebas preliminares, en las que se mandaba 100000
 * paquetes tan rápido como es posible, por UDP de una máquina a sí
 * misma.
 * Si el receptor era syslog-ng, llegaban alrededor de un 30%;
 * si el receptor era un Scoja primitivo con destino un
 * {@link FileTarget}, llegaban alrededor de un 60%;
 * pero si el destino era un objeto de esta clase abriendo y cerrando
 * los ficheros para cada paquete, llegaban a lo más un 3%.
 * <p>
 * La manera razonable de resolver esto es mediante una caché de
 * ficheros; que en nuestro caso la implementa {@link FileLRUCache}.
 * Obviamente, todos los objetos que escriban en fichero deben
 * compartir esta caché para que no habran de forma separada el mismo
 * fichero y se pisen lo que allí escriban.
 */
public class TemplateFileTarget extends FileTarget {
    
    protected Template template;
    
    public TemplateFileTarget(final FileSystem fileSystem,
                              final ExpiringLRUCache fileCache) {
        super(fileSystem, fileCache);
        this.template = null;
    }
    
    public void setName(final Template template) {
        this.template = template;
    }

    protected String getFilename(final EventContext env) {
        final String filename = template.toFilename(env);
        //System.err.println(template + " -> " + filename);
        return filename;
    }
}
