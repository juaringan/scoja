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

package org.scoja.server.parser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.scoja.trans.RemoteInfo;
import org.scoja.common.PriorityUtils;
import org.scoja.server.core.ScojaThread;
import org.scoja.server.core.Link;
import org.scoja.server.core.Event;
import org.scoja.server.core.EventContext;
import org.scoja.server.source.Internal;

/**
 * Split a continuous data source in a sequence of events.
 * Events are splitted at <i>end of lines</i> or at end of source.
 * \0, \r and \n are considered event separators.
 * White space is removed at the beginning of events.
 * Empty events (or formed exclusively with white space) are
 * discarded.
 *
 * <p>Found events are send to the {@link Link} given at construction.
 *
 * @todo
 * Abstract away event terminator, so that Scoja can receive events
 * with a more complex syntax.
 *
 * @implementation
 * <pre>
|<------------------------ data.length ----------------------->|
|                                                              |
+--------------+-----------------+-------------+---------------+
|              |                 |             |               |
+--------------+-----------------+-------------+---------------+
                ^                 ^             ^
                |                 |             |
              init              parsed         end
 * </pre>
 * Usually {@link #parsed} is equal to
 * {@link #init} or to {@link #end}.
 * When adding, data is copied after {@link #end}, moving it appart
 * from {@link #parsed}.
 * Just after adding, a call to {@link #split()} moves
 * {@link #parsed}, looking for a separator upto {@link #end}.
 */
public class EventSplitter {

    protected final RemoteInfo peer;
    //protected final EventSeparator separator;
    protected final EventParser eventParser;
    protected final Link link;

    protected int inbuffer;
        
    protected byte[] data;
    protected ByteBuffer bdata;
    protected int init;
    protected int parsed;
    protected int end;
    
    public EventSplitter(final RemoteInfo peer,
                         final Link link) {
        this(peer, StdSyslogEventParser.getInstance(), link);
    }
    
    public EventSplitter(final RemoteInfo peer,
                         //final EventSeparator separator,
                         final EventParser eventParser,
                         final Link link) {
        this.peer = peer;
        this.eventParser = eventParser;
        this.link = link;

        this.inbuffer = 16*1024;
                
        this.data = null;
        this.bdata = null;
        this.init = this.parsed = this.end = 0;
    }
    
    public void setInBuffer(final int inbuffer) {
        this.inbuffer = Math.max(1,inbuffer);
    }
    
    public synchronized void add(final byte[] source) {
        add(source, 0, source.length);
    }
    
    public synchronized void add(final byte[] source,
                                 final int offset, final int length) {
        ensure(length);
        System.arraycopy(source,offset, data,end,length);
        end += length;
        split();
    }
    
    public synchronized int add(final ReadableByteChannel source)
    throws IOException {
        int totalReaded = 0;
        shiftEmpty();
    reading:
        for (;;) {
            ensure();
            final int capacity = data.length - end;
            bdata.position(end);
            final int readed = source.read(bdata);
            if (readed > 0) {
                totalReaded += readed;
                end += readed;
                split();
            } else {
                if (readed < 0) source.close();
                break reading;
            }
        }
        return totalReaded;
    }
    
    public synchronized void flush() {
        split();
        if (init < parsed) {
            process();
            init = parsed;
        }
    }
    
    protected void shiftEmpty() {
        if (init == end) {
            init = parsed = end = 0;
        }
    }
    
    protected void ensure() {
        ensure0(inbuffer);
    }
    
    protected void ensure(final int needed) {
        ensure0(Math.max(needed, inbuffer));
    }
    
    protected void ensure0(final int needed) {
        final int len = (data == null) ? 0 : data.length;
        if (needed <= len - end) return;
        final int used = end-init;
        final int available = len - used;
        if (needed <= available && used <= available) {
            System.arraycopy(data,init, data,0,used);
        } else {
            final int newSize = used+Math.max(needed,used);
            //System.err.println(
            //    "Resizing EventSplitter:"
            //    + " size=" + len + ", used=" + used
            //    + ", needed=" + needed + ", new size=" + newSize);
            final byte[] newData = new byte[newSize];
            if (used > 0) System.arraycopy(data,init, newData,0,used);
            data = newData;
            bdata = ByteBuffer.wrap(data);
        }
        parsed -= init;
        init = 0;
        end = used;
    }
    
    protected void split() {
        while (parsed < end) {
            if (init == parsed) {
                while (init < end && data[init] <= ' ') init++;
                parsed = init;
            }
            for (;;) {
                if (parsed >= end) break;
                if (data[parsed] == '\0'
                    || data[parsed] == '\n'
                    || data[parsed] == '\r') {
                    process();
                    parsed++;
                    init = parsed;
                    break;
                }
                parsed++;
            }
        }
    }
    
    protected void process() {
        final Event event
            = new ParsedEvent(eventParser, peer, data, init, parsed-init);
        final EventContext ectx = new EventContext(event);
        ScojaThread.setCurrentEventContext(ectx);
        if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
            Internal.debug(ectx, Internal.PARSER, "Received: " + ectx);
        }
        link.process(ectx);
    }    
}
