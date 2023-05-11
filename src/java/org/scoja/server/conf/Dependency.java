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

package org.scoja.server.conf;

import java.io.File;
import java.io.IOException;

public class Dependency {

    protected final File file;
    protected long previousModification;
    
    public Dependency(final String filename) throws IOException {
        this(new File(filename).getCanonicalFile());
    }

    public Dependency(final File file) {
        this.file = file;
        this.previousModification = -1;
    }

    public File getFile() {
        return file;
    }
        
    public boolean hasChanged() {
        return previousModification < file.lastModified();
    }
    
    public void updateModification() {
        this.previousModification = file.lastModified();
    }
}
