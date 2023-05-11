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

import java.net.URL;

public class URITest {

    public static void main(final String[] args)
    throws Exception {
        //final URL u = new URL(null, args[0], new Handler());
        final URL u = new URL(null, args[0]);
        System.out.println(u);
        System.out.print(
            "Query: " + u.getQuery()
            + "\npath: " + u.getPath()
            + "\nuser info: " + u.getUserInfo()
            + "\nauthority: " + u.getAuthority()
            + "\nport: " + u.getPort()
            + "\ndefault port: " + u.getDefaultPort()
            + "\nprotocol: " + u.getProtocol()
            + "\nhost: " + u.getHost()
            + "\nfile: " + u.getFile()
            + "\nanchor: " + u.getRef()
            + "\n");
    }
}
