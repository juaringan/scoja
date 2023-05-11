/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2008  Bankinter, S.A.
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

package org.scoja.server.parser;

import java.io.OutputStream;

import org.scoja.trans.RemoteInfo;
import org.scoja.server.core.Link;

public class LightProtocolFactory implements ProtocolFactory {

    private static final LightProtocolFactory instance
        = new LightProtocolFactory();
        
    public static LightProtocolFactory getInstance() { return instance; }

    public String getName() { return "traditional"; }
    public String getDescription() {
        return "The traditional BSD syslog protocol";
    }
    
    public PacketProtocol newPacketProtocol(final Link link) {
        return new EventPerPacketProtocol(
            StdSyslogEventParser.getInstance(), link);
    }
    
    public StreamProtocol newStreamProtocol(final RemoteInfo peer,
                                            final Link link, 
                                            final OutputStream out) {
        return new LightStreamProtocol(peer, link);
    }
}
