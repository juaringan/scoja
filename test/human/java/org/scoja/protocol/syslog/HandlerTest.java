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

package org.scoja.protocol.syslog;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class HandlerTest {

    public static void main(final String[] args)
    throws Exception {
        final URL u = new URL(args[0]);
        final OutputStream out = u.openConnection().getOutputStream();
        final InputStream in = System.in;
        final byte[] buffer = new byte[1024];
        for (;;) {
            final int read = in.read(buffer);
            if (read == -1) break;
            out.write(buffer, 0, read);
        }
        out.close();
    }
}
