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

import junit.framework.*;
import junit.textui.TestRunner;

import java.io.StringReader;
import java.text.SimpleDateFormat;

/**
 */
public class EventSourceTest extends TestCase {

    public static void main(final String args[]) {
        TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(EventSourceTest.class);
    }
    
    public EventSourceTest(final String name) {
        super(name);
    }
    
    protected void setUp() {
    }

    protected void tearDown() {
    }
    
    private static final Format standardFormat
        = new Format(new LiteralLocator("\n"),
                     new AtInitLocator(),
                     new JumpLocator("Jan 25 07:11:19".length()),
                     "MMM dd HH:mm:ss");

    private static final String data1
        = "Jan 25 07:11:19 palastro (p-1637): starting (version 2.4.0.1), pid 1637 user 'p'\n"
        + "Jan 25 07:11:21 palastro (p-1637): Resolved address \"xml:readonly:/etc/gconf/gconf.xml.mandatory\" to a read-only config source at position 0\n"
        + "Jan 25 07:11:23 palastro (p-1637): Resolved address \"xml:readwrite:/home/p/.gconf\" to a writable config source at position 1\n"
        + "Jan 25 07:11:25 palastro (p-1637): Resolved address \"xml:readonly:/etc/gconf/gconf.xml.defaults\" to a read-only config source at position 2\n";

    private static final String data2
        = "Jan 25 07:11:20 palastro (p-1637): starting (version 2.4.0.1), pid 1637 user 'p'\n"
        + "Jan 25 07:11:22 palastro (p-1637): Resolved address \"xml:readonly:/etc/gconf/gconf.xml.mandatory\" to a read-only config source at position 0\n"
        + "Jan 25 07:11:24 palastro (p-1637): Resolved address \"xml:readwrite:/home/p/.gconf\" to a writable config source at position 1\n"
        + "Jan 25 07:11:26 palastro (p-1637): Resolved address \"xml:readonly:/etc/gconf/gconf.xml.defaults\" to a read-only config source at position 2\n";

    public void testParsing()
    throws Exception {
        final EventSource source
            = new ReaderEventSource(standardFormat, new StringReader(data1));
        for (;;) {
            source.advance();
            if (!source.has()) break;
            System.out.println(source.current());
        }
    }

    public void testMixing()
    throws Exception {
        final EventSource source
            = new MixingEventSource(new EventSource[] {
                new ReaderEventSource(standardFormat, new StringReader(data1)),
                new ReaderEventSource(standardFormat, new StringReader(data2)),
            });
        for (;;) {
            source.advance();
            if (!source.has()) break;
            System.out.println(source.current());
        }
    }
}
