/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003  Mario Martínez
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

package org.scoja.io;

import java.util.Random;

public class ExhaustiveDataStorage
    implements DataStorage {
    
    protected final byte[][] data;
    
    public ExhaustiveDataStorage(final int n,
                                 final int minsize, final int maxsize) {
        data = new byte[n][];
        final Random r = new Random();
        for (int i = 0; i < n; i++) {
            final int size = minsize + ((maxsize-minsize)*i + n/2)/n;
            data[i] = new byte[size];
            r.nextBytes(data[i]);
        }
    }
    
    public byte[] get(final int n) {
        return data[n % data.length];
    }
}
