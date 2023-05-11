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
import java.nio.ByteOrder;

import org.scoja.cc.util.XArray;

import org.scoja.common.PriorityUtils;
import org.scoja.trans.RemoteInfo;
import org.scoja.server.core.Link;
import org.scoja.server.core.EventContext;
import org.scoja.server.core.HidingEnvironment;
import org.scoja.server.parser.PacketProtocol;
import org.scoja.server.source.Internal;

public class NetflowProtocol implements PacketProtocol {

    protected final Link link;
    
    public NetflowProtocol(final Link link) {
        this.link = link;
    }

    public void processPacket(final RemoteInfo peer,
            final byte[] data, final int off, final int len) {
        if (len < 2) {
            if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
                Internal.debug(Internal.SOURCE_UDP,
                        "Netflow packet too short (" + len + ")");
            }
            return;
        }
        //Copy data to work correctly with queues
        final byte[] buffer = XArray.subarray(data, off, len);
        final ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.BIG_ENDIAN);
        final int version = BBTrait.getUShort(bb, 0);
        switch (version) {
        case 1: processSimple(NetflowV1.getInstance(), peer, bb); break;
        case 5: processSimple(NetflowV5.getInstance(), peer, bb); break;
        case 6: processSimple(NetflowV6.getInstance(), peer, bb); break;
        case 7: processSimple(NetflowV7.getInstance(), peer, bb); break;
        default: unsupportedVersion(version, peer, bb); break;
        }
    }

    protected void processSimple(final NetflowVSimple netflow,
            final RemoteInfo peer, final ByteBuffer bb) {
        final int flows = netflow.flows(bb);
        if (flows < 0) {
            if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
                Internal.debug(Internal.SOURCE_UDP,
                        "Malformet netflow packet (version"
                        + netflow.version() + ")");
            }
            return;
        }
        int foff = netflow.headerSize();
        for (int i = 0; i < flows; i++) {
            final NetflowEnvironment env = netflow.environment(i, bb, 0, foff);
            final EventContext ectx = new EventContext(
                new NetflowEvent(peer, env),
                new HidingEnvironment(env));
            link.process(ectx);
            foff += netflow.flowSize();
        }
    }
        
    protected void unsupportedVersion(final int version, final RemoteInfo peer,
            final ByteBuffer bb) {
        if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
            Internal.debug(Internal.SOURCE_UDP,
                    "Netflow packet with unsupported version " + version);
        }
    }
}
