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

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

import org.scoja.trans.RemoteInfo;
import org.scoja.server.core.Link;

public class LightStreamProtocol implements StreamProtocol {
    
    protected final RemoteInfo peer;
    protected final Link link;
    protected final EventSplitter splitter;
    
    public LightStreamProtocol(final RemoteInfo peer, final Link link) {
        this.peer = peer;
        this.link = link;
        this.splitter = new EventSplitter(peer, link);
    }
    
    public void setInBuffer(final int inbuffer) {
        splitter.setInBuffer(inbuffer);
    }

    public void processAvailable(final ReadableByteChannel source)
    throws IOException {
        splitter.add(source);
    }
    
    public void flush() {
        splitter.flush();
    }
}
