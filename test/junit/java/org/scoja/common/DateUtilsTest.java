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

package org.scoja.common;

import junit.framework.*;
import junit.textui.TestRunner;

import java.util.Calendar;

public class DateUtilsTest extends TestCase {

    public static void main(final String args[]) {
        TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(DateUtilsTest.class);
    }
    
    public DateUtilsTest(final String name) {
        super(name);
    }
    
    protected void setUp() {
    }

    protected void tearDown() {
    }
    
    public void testFormat()
    throws Exception {
        final Calendar cal = Calendar.getInstance();
        final StringBuilder sb = new StringBuilder();
        sb.append("Std: "); DateUtils.formatStd(sb, cal);
        sb.append("\nAbbreviated: "); DateUtils.formatAbbreviated(sb, cal);
        sb.append("\nLong: "); DateUtils.formatLong(sb, cal);
        sb.append("\nGMT: "); DateUtils.formatGMT(sb, cal);
        System.err.println(sb.toString());
    }
}
