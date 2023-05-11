/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2012  LogTrust
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

package org.scoja.server.netflow;

import java.nio.ByteBuffer;

public class Flow {

    protected final int rseq;
    protected final ByteBuffer bb;
    protected final int hoff;  //Header offset
    protected final int foff;  //Flow data offset

    public Flow(final int rseq, 
            final ByteBuffer bb, final int hoff, final int foff) {
        this.rseq = rseq;
        this.bb = bb;
        this.hoff = hoff;
        this.foff = foff;
    }
    
    public int relativeFlowSeq() { return rseq; }
    
    protected int base(final boolean header) {
        return header ? hoff : foff;
    }
    
    protected int position(final boolean header, final int pos) {
        return base(header) + pos;
    }
    
    public long getUInt(final boolean header, final int pos) {
        return BBTrait.getUInt(bb, position(header,pos));
    }
    
    public int getUShort(final boolean header, final int pos) {
        return BBTrait.getUShort(bb, position(header,pos));
    }
    
    public int getUByte(final boolean header, final int pos) {
        return BBTrait.getUByte(bb, position(header,pos));
    }
}
