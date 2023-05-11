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

package org.scoja.io.posix;

import junit.framework.*;
import junit.textui.TestRunner;

import java.net.DatagramPacket;

/**
 * This class tests {@link org.scoja.io.posix.FileMode}.
 */
public class FileModeTest extends TestCase {

    public static void main(final String args[]) {
        TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(FileModeTest.class);
    }
    
    public FileModeTest(final String name) {
        super(name);
    }
    
    protected void setUp() {
    }

    protected void tearDown() {
    }

    /**
     * Test each permission individually.
     * Passing this test means that conversion to string and parsing work
     * correctly for each permission.
     */
    public void testPermissionParts()
    throws Exception {
        for (int i = 0; i < 4*3; i++) {
            final FileMode m0 = new FileMode(1 << i);
            final FileMode m1 = new FileMode(m0.toString());
            assertEquals(m0, m1);
        }
    }
    
    /**
     * Test conversion to string and parsing for every permission.
     */
    public void testAllPermissions()
    throws Exception {
        for (int i = 0; i <= 07777; i++) {
            final FileMode m0 = new FileMode(i | FileMode.IFREG);
            final FileMode m1 = new FileMode(m0.toString());
            assertEquals(m0, m1);
        }
    }
}
