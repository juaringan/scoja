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

import java.io.IOException;

/**
 * The way to use this interface is:
 * <pre><tt>
 * for (;;) {
 *    main.advance();
 *    if (!main.has()) break;
 *    final Event e = main.current();
 *    //Do something with e
 * }
 * </tt></pre>
 */
public interface EventSource {

    public void advance()
    throws IOException;
    
    public boolean has();
    
    public Event current();
}
