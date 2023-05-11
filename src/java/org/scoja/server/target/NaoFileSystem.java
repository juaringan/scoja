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

import java.io.IOException;
import java.io.File;
import org.scoja.comp.arch.nao.ModelBoard;
import org.scoja.comp.arch.nao.NaoOutputStream;

public class NaoFileSystem extends LocalFileSystem {

    public static final String DEFAULT_EXT
        = ".nao";
    public static final ModelBoard DEFAULT_MODELS
        = new ModelBoard.OnMap().forNao();
    public static final int DEFAULT_FILTER
        = ModelBoard.OLZO1H_ID;
    public static final int[] DEFAULT_HASHERS
        = new int[] {ModelBoard.CRC32_ID};
    public static final int DEFAULT_CHUNKSIZE
        =  NaoOutputStream.DEFAULT_CHUNK_SIZE;
    public static final int[] DEFAULT_INDEXCOUNTS
        = new int[] {1 << 6, 1 << 8};

    protected String ext;
    protected ModelBoard models;
    protected int filter;
    protected int[] hashers;
    protected int chunkSize;
    protected int[] indexCounts;
    
    public NaoFileSystem() {
        this.ext = DEFAULT_EXT;
        this.models = DEFAULT_MODELS;
        this.filter = DEFAULT_FILTER;
        this.hashers = DEFAULT_HASHERS;
        this.chunkSize = DEFAULT_CHUNKSIZE;
        this.indexCounts = DEFAULT_INDEXCOUNTS;
    }
    
    public void setExtension(final String ext) {
        this.ext = ext;
    }
    
    public void setModels(final ModelBoard models) {
        this.models = models;
    }
    
    public void setFilter(final int filter) {
        this.filter = filter;
    }
    
    public void setHashers(final int[] hashers) {
        this.hashers = hashers;
    }
    
    public void setChunkSize(final int chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    public void setIndexCounts(final int[] indexCounts) {
        this.indexCounts = indexCounts;
    }

    public BinaryFile openBinary(final String filename,
                                 final FileBuilding building) {
        return filename.endsWith(ext)
            ? new NaoFile(this, filename, building)
            : new PlainFile(this, filename, building);
    }
    
    protected NaoOutputStream openAppend(final File filename)
    throws IOException {
        final NaoOutputStream out 
            = new NaoOutputStream(filename, true, models);
        out.setFilters(new int[] {filter});
        out.setHashers(hashers);
        out.setChunkSize(chunkSize);
        out.setIndexSizes(indexCounts);
        return out;
    }
    
}    
