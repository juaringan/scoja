/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2010  Mario Martínez
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
package org.scoja.trans;

import junit.framework.*;
import junit.textui.TestRunner;

import java.util.List;

import org.scoja.cc.lang.Structural;
import org.scoja.cc.io.Charsets;

public class IncrementalParserTest extends TestCase {

    public static void main(final String args[]) {
        TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(IncrementalParserTest.class);
    }
    
    public IncrementalParserTest(final String name) {
        super(name);
    }
    
    protected void setUp() {
    }

    protected void tearDown() {
    }

    public void testHttpResponseStatus()
    throws Exception {
        final IncrementalParser<List<Object>> ip = httpResponseStatusParser();
        final String[] statusLines = {
            "HTTP/1.0 200 Connection established\r\n",
        };
        for (final String line: statusLines) {
            final byte[] bs = Charsets.latin1(line);
            final int n0 = ip.parse(bs, 0, bs.length);
            assertTrue(ip.isDone());
            assertEquals(bs.length, n0);
            final List<Object> vs0 = ip.getParsedValue();
            ip.reset();
            int n1 = 0;
            for (int i = 1; i <= bs.length; i++) {
                final int p = ip.parse(bs, n1, i-n1);
                n1 += p;
            }
            assertTrue(ip.isDone());
            assertEquals(bs.length, n1);
            final List<Object> vs1 = ip.getParsedValue();
            //assertEquals(vs0, vs1);
            assertTrue(Structural.equals(vs0.toArray(), vs1.toArray()));
            ip.reset();
        }
    }
    
    protected IncrementalParser<List<Object>> httpResponseStatusParser() {
        return new IncrementalParser.Seq<Object>(
            new IncrementalParser[] {
                new IncrementalParser.Literal("HTTP/"),
                new IncrementalParser.Nat(),
                new IncrementalParser.Literal("."),
                new IncrementalParser.Nat(),
                new IncrementalParser.Space(),
                new IncrementalParser.Nat(),
                new IncrementalParser.Space(),
                new IncrementalParser.Upto("\r\n", true),
            });
    }
}
