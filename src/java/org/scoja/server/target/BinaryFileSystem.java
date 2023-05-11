/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2008  Bankinter, S.A.
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

import java.nio.charset.Charset;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public abstract class BinaryFileSystem implements FileSystem {

    protected Charset charset;
    protected int inactivityAfterError;
    public static final int DEFAULT_INACTIVITY_AFTER_ERROR = 12*1000;
    
    public BinaryFileSystem() {
        this.charset = Charset.defaultCharset();
        this.inactivityAfterError = DEFAULT_INACTIVITY_AFTER_ERROR;
    }
    
    public void setCharset(final Charset charset) {
        this.charset = charset;
    }
    
    public void setInativityAfterError(final int inactivityAfterError) {
        this.inactivityAfterError = inactivityAfterError;
    }

    public TextFile openText(final String filename, 
                             final FileBuilding building,
                             final Flushing flushing) {
        final BinaryFile bin = openBinary(filename, building);
        Writer writer = new OutputStreamWriter(bin.getOut(), charset);
        final int bufferSize = flushing.getBufferSize();
        if (bufferSize > 0) {
            writer = new BufferedWriter(writer);
        }
        return new TextFile.Canonical(bin, writer);
    }
    
    public abstract BinaryFile openBinary(String filename,
                                          FileBuilding building);
}
