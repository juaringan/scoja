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
package org.scoja.trans;

import java.io.IOException;

/**
 * This interface is synchronous: the server blocks until the binding is
 * fully done or a connection is received.
 * There is no real need for a non-blocking binding or connection receception.
 * Nevertheless, we need to know when there is a pending connection request:
 * it must be solved with a Selector.
 */
public interface TransportService<C> extends Selectable {

    public C configuration()
    throws IOException;

    public boolean isBound()
    throws IOException;
    
    public void bind()
    throws IOException;
    
    public void close()
    throws IOException;
    
    public TransportLine<C> accept()
    throws IOException;
    
    
    //======================================================================
    public static class TypeAdaptor<C> implements TransportService<C> {
        protected final TransportService<?> base;
        
        public TypeAdaptor(final TransportService<?> base) {
            this.base = base;
        }
        
        public C configuration() { return null; }
        
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
        
        public TransportLine<C> accept()
        throws IOException {
            return new TransportLine.TypeAdaptor<C>(base.accept());
        }
        
        public SelectionHandler register(final SelectionHandler handler){
            handler.addSelectable(this);
            return base.register(handler);
        }
        
        public String toString() {
            return base.toString();
        }
    }
}
