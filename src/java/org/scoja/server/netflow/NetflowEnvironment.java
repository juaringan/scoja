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

import java.io.IOException;
import java.util.Map;

import org.scoja.server.core.Environment;
import org.scoja.server.core.QStr;

public class NetflowEnvironment implements Environment {

    protected QStr unknown;
    protected NetflowVSimple netflow;
    protected final Flow flow;
    protected final Map<String,Field> var2field;

    public NetflowEnvironment(final NetflowVSimple netflow,
            final Flow flow, 
            final Map<String,Field> var2field) {
        this.unknown = Q_UNKNOWN;
        this.netflow = netflow;
        this.flow = flow;
        this.var2field = var2field;
    }
    
    public void mark() {
        throw new UnsupportedOperationException();
    }
    
    public void release() {
        throw new UnsupportedOperationException();
    }

    public boolean isDefined(final String var) {
        return var2field.containsKey(var);
    }

    public void define(final String var, final String value) {
        throw new UnsupportedOperationException();
    }
        
    public void define(final String var, final QStr value) {
        throw new UnsupportedOperationException();
    }
    
    public QStr definition(final String var) {
        final Field field = var2field.get(var);
        return (field == null) ? null : new QStr(field.repr(flow));
    }
    
    public void unknown(final String value) { unknown(new QStr(value)); }
    
    public void unknown(final QStr value) { this.unknown = value; }
    
    public QStr unknown() { return unknown; }

    public int byteSize() { return netflow.flowSize(); }
    
    public long sendTimestamp() { return netflow.sendTimestamp(flow); }
        
    public void writeTo(final Appendable sb)
    throws IOException {
        boolean first = true;
        for (final Map.Entry<String,Field> entry: var2field.entrySet()) {
            if (first) first = false; else sb.append(", ");
            sb.append(entry.getKey()).append('=')
                .append(entry.getValue().repr(flow));
        }
    }
    
    public String toString() {
        return "NetflowEnvironment["
            + "flow: " + flow
            + ", fields: " + var2field
            + "]";
    }
}