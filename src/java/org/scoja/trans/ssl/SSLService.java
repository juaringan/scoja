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
package org.scoja.trans.ssl;

import java.io.IOException;

import org.scoja.cc.lang.Exceptions;

import org.scoja.trans.TransportService;
import org.scoja.trans.TransportLine;
import org.scoja.trans.SelectionHandler;
import org.scoja.trans.lc.LCLine;

public class SSLService implements TransportService<SSLConf> {

    protected final SSLTransport trans;
    protected final SSLConf.Stacked conf;
    protected final TransportService base;

    public SSLService(final SSLTransport trans) {
        this.trans = trans;
        this.conf = new SSLConf.Stacked(trans.conf);
        this.base = trans.base.server();
    }
    
    public SSLConf configuration() { return conf; }

    public boolean isBound()
    throws IOException {
        return base.isBound();
    }
    
    public void bind()
    throws IOException {
        base.bind();
    }
    
    public void close()
    throws IOException {
        base.close();
    }
    
    public TransportLine<SSLConf> accept()
    throws IOException {
        final TransportLine baseline = base.accept();
        try {
            return new LCLine<SSLConf>(new SSLLine.Server(this, baseline));
        } catch (Throwable e) {
            baseline.close();
            throw Exceptions.uncheckedOr(IOException.class, e);
        }
    }
    
    public SelectionHandler register(final SelectionHandler handler) {
        handler.addSelectable(this);
        return base.register(handler);
    }    
    
    public String toString() {
        return "SSLService[from: " + trans
            + ", with: " + conf
            + "]";
    }
}
