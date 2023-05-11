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
package org.scoja.server.source;

import java.io.IOException;

/**
 * We are trying to do a open sockets cache.
 * This improvement is going to be delayed because
 * it is not trivial and current behaviour is good enough.
 */
public interface Spring {

    public Id getId();
    
    public boolean isOpen();
    
    public void open() throws IOException;
    
    public void bind(ContinuationProvider cont);
    
    public void processEntry();
    
    public void unbind();
    
    public void close() throws IOException;
    
    
    //======================================================================
    public static interface Id {}
}
