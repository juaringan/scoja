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

/**
 * A trait for ByteBuffer with unsignned reading operations.
 * Should be in common.
 */
public class BBTrait {

    public static final int UBYTE_MASK = 0xFF;
    
    public static int getUByte(final ByteBuffer bb) {
        return bb.get() & UBYTE_MASK;
    }
    
    public static int getUByte(final ByteBuffer bb, final int i) {
        return bb.get(i) & UBYTE_MASK;
    }

    public static final int USHORT_MASK = 0xFFFF;

    public static int getUShort(final ByteBuffer bb) {
        return bb.getChar();
    }
    
    public static int getUShort(final ByteBuffer bb, final int i) {
        return bb.getChar(i);
    }
    
    public static final long UINT_MASK = 0xFFFFFFFFl;
    
    public static long getUInt(final ByteBuffer bb) {
        return bb.getInt() & UINT_MASK;
    }
    
    public static long getUInt(final ByteBuffer bb, final int i) {
        return bb.getInt(i) & UINT_MASK;
    }
}