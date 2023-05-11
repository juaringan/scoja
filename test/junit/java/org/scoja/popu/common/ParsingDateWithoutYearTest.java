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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Test {@link SimpleDateFormat} behaviour when a date lacks some items.
 * Coherent behaviour when year is absent is required because standard
 * syslog dates have not year.
 * Fortunately, year defaults to 1970, which is enough for <tt>recoja</tt>
 * to work.
 */
public class ParsingDateWithoutYearTest extends TestCase {

    public static void main(final String args[]) {
        TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(ParsingDateWithoutYearTest.class);
    }
    
    public ParsingDateWithoutYearTest(final String name) {
        super(name);
    }
    
    protected void setUp() {
    }

    protected void tearDown() {
    }

    public void testParsing()
    throws Exception {
        final SimpleDateFormat syslogParser
            = new SimpleDateFormat("MMM dd HH:mm:ss");
        final SimpleDateFormat fullParser
            = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        assertEquals(fullParser.parse("1970-01-21 07:18:55"),
                     syslogParser.parse("Jan 21 07:18:55"));
    }
}
