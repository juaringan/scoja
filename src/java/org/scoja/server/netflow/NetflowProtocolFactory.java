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

import java.io.OutputStream;

import org.scoja.trans.RemoteInfo;
import org.scoja.server.core.Link;
import org.scoja.server.parser.ProtocolFactory;
import org.scoja.server.parser.StreamProtocol;

public class NetflowProtocolFactory implements ProtocolFactory {
    
    private static final NetflowProtocolFactory instance
        = new NetflowProtocolFactory();
        
    public static NetflowProtocolFactory getInstance() { return instance; }
    
    public String getName() { return "netflow"; }
    public String getDescription() { return "Netflow protocol"; }

    public NetflowProtocol newPacketProtocol(final Link link) {
        return new NetflowProtocol(link);
    }
    
    public StreamProtocol newStreamProtocol(
        final RemoteInfo peer, final Link link, final OutputStream out) {
        throw new UnsupportedOperationException(
            "Netflow is not defined on top of TCP");
    }
}
