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

import java.nio.channels.ReadableByteChannel;

import org.scoja.common.PriorityUtils;
import org.scoja.trans.RemoteInfo;
import org.scoja.server.core.Event;
import org.scoja.server.core.EventContext;
import org.scoja.server.core.Link;
import org.scoja.server.core.ScojaThread;
import org.scoja.server.source.Internal;


public class EventPerPacketProtocol implements PacketProtocol {
    
    protected final EventParser eventParser;
    protected final Link link;

    public EventPerPacketProtocol(final EventParser eventParser,
                                  final Link link) {
        this.eventParser = eventParser;
        this.link = link;
    }
        
    public void processPacket(final RemoteInfo peer,
                              final byte[] data, final int off, final int len){
        final Event event 
            = new ParsedEvent(eventParser, peer, data, off, len);
        final EventContext ectx = new EventContext(event);
        ScojaThread.setCurrentEventContext(ectx);
        if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
            Internal.debug(ectx, Internal.SOURCE_UDP,
                    "Received: " + ectx);
        }
        link.process(ectx);
    }
}
