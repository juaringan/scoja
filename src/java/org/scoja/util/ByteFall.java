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

package org.scoja.util;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import org.scoja.trans.OStream;

public interface ByteFall {
    
    public boolean isEmpty();
    
    public int itemCount();
    
    public int size();
    
    public int add(byte[] bs, int off, int len);
    
    public int add(boolean partial, byte[] bs, int off, int len);
    
    public int dropPartial();
    
    public int unload(WritableByteChannel out) throws IOException;
    
    public int unload(OStream out) throws IOException;
}
