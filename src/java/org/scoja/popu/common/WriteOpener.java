/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
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

package org.scoja.popu.common;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

public interface WriteOpener {

    public Writer open(String filename)
    throws IOException;
    
    //======================================================================
    public static class ForGZip
        implements WriteOpener {
        
        public Writer open(final String filename)
        throws IOException {
            if (filename.endsWith(".gz")) {
                final OutputStream in = new FileOutputStream(filename);
                try {
                    return new OutputStreamWriter(new GZIPOutputStream(in));
                } catch (IOException e) {}
            }
            return null;
        }
    }
}
