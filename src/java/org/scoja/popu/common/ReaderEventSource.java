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

package org.scoja.popu.common;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.Date;
import org.scoja.common.DateLayout;

public class ReaderEventSource
    implements EventSource {
    
    protected final Format format;
    protected final Locator eventEnd;
    protected final Locator dateStart;
    protected final Locator dateEnd;
    protected final DateLayout dateFormat;
    
    protected final Reader source;
    protected boolean sourceEnded;
    protected char[] buffer;
    protected int next;
    protected int inUse;
    
    protected final Event myBelovedEvent;
    protected Event current;
    
    public ReaderEventSource(final Format format,
                             final Reader source) {
        this.format = format;
        this.eventEnd = format.getEventEnd();
        this.dateStart = format.getDateStart();
        this.dateEnd = format.getDateEnd();
        this.dateFormat = format.getDateFormat();
        
        this.source = source;
        this.sourceEnded = false;
        this.buffer = new char[010*1024];
        this.next = 0;
        this.inUse = 0;
        
        this.myBelovedEvent = new Event(buffer);
        this.current = null;
    }
    
    public void advance()
    throws IOException {
        current = null;
        
        eventEnd.reset();
        for (;;) {
            eventEnd.locate(buffer, next, inUse);
            if (eventEnd.located()) break;
            reload();
            if (sourceEnded) break;
        }
        final int currentStart = next;
        final int currentEnd = next
            = eventEnd.located() ? eventEnd.end() : inUse;
        if (currentStart == currentEnd && sourceEnded) return;
        myBelovedEvent.with(currentStart, currentEnd);
        
        dateStart.reset();
        dateStart.locate(buffer, currentStart, currentEnd);
        if (dateStart.located()) {
            dateEnd.reset();
            dateEnd.locate(buffer, dateStart.end(), currentEnd);
            if (dateEnd.located()) {
                try {
                    final int ds = dateStart.end();
                    final int de = dateEnd.init();
                    final Date date
                        = dateFormat.parse(new String(buffer, ds, de-ds));
                    myBelovedEvent.with(date.getTime(), ds, de);
                } catch (ParseException e) {}
            }
        }
        current = myBelovedEvent;
    }
    
    protected void reload()
    throws IOException {
        final int remain = inUse - next;
        if (4*remain <= buffer.length) {
            System.arraycopy(buffer,next, buffer,0,remain);
        } else {
            final char[] newBuffer = new char[2*buffer.length];
            System.arraycopy(buffer,next, newBuffer,0,remain);
            buffer = newBuffer;
            myBelovedEvent.with(newBuffer);
        }
        next = 0;
        inUse = remain;
        final int toRead = buffer.length - inUse;
        final int read = source.read(buffer, inUse, toRead);
        if (read == -1) {
            sourceEnded = true;
        } else {
            inUse += read;
        }
    }
    
    public boolean has() {
        return current != null;
    }
    
    public Event current() {
        return current;
    }
}
