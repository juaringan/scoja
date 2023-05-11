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

import java.util.Map;
import java.nio.ByteBuffer;

public class NetflowVSimple {

    protected final int version;
    protected final int headerSize;
    protected final int flowSize;
    protected final Field flowsField;
    protected final Field epochField;
    protected final Map<String,Field> fieldsByName;
    
    public NetflowVSimple(final int version, final int headerSize,
            final int flowSize, 
            final Field flowsField, final Field epochField,
            final Map<String,Field> fieldsByName) {
        this.version = version;
        this.headerSize = headerSize;
        this.flowSize = flowSize;
        this.flowsField = flowsField;
        this.epochField = epochField;
        this.fieldsByName = fieldsByName;
    }
    
    public int version() { return version; }
    public int headerSize() { return headerSize; }
    public int flowSize() { return flowSize; }
    
    public int flows(final ByteBuffer bb) {
        final long declared = flowsField.value(bb);
        final int possible = (bb.limit() < headerSize) ? 0
            : ((bb.limit() - headerSize) / flowSize);
        return (int)Math.max(declared, possible);
    }
    
    public long sendTimestamp(final Flow flow) {
        return epochField.value(flow);
    }
    
    public NetflowEnvironment environment(final int rseq,
            final ByteBuffer bb, final int hoff, final int foff) {
        return new NetflowEnvironment(
            this, new Flow(rseq, bb,hoff,foff), fieldsByName);
    }
    
    public String toString() {
        return "Netflow (version " + version + ")";
    }
}

