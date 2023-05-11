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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

public interface ReadOpener {

    public Reader open(String filename)
    throws IOException;
    
    //======================================================================
    public static class ForGZip
        implements ReadOpener {
        
        public Reader open(final String filename)
        throws IOException {
            //System.out.println("ReadOpener.ForGZip: " + filename);
            if (filename.endsWith(".gz")) {
                final InputStream in = new FileInputStream(filename);
                try {
                    return new InputStreamReader(new GZIPInputStream(in));
                } catch (IOException e) {}
            }
            return null;
        }
    }
}
