/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2010  Mario Martínez
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
package org.scoja.trans.lc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ByteChannel;

import org.scoja.trans.ConnectState;
import org.scoja.trans.TransportLine;

public abstract class NoLCLine<C> implements TransportLine<C> {

    public ConnectState connectState() {
        throw noLifeCycle();
    }
    
    public InputStream inputStream()
    throws IOException {
        throw noLifeCycle();
    }        
    
    public OutputStream outputStream()
    throws IOException {
        throw noLifeCycle();
    }
        
    public ByteChannel channel()
    throws IOException {
        throw noLifeCycle();
    }

    protected UnsupportedOperationException noLifeCycle() {
        return new UnsupportedOperationException(
            "Partial implementation without life-cycle control"
            + "; should be proxied with `" + LCLine.class.getName() + "'");
    }
}
