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

package org.scoja.util;

import java.util.regex.*;

import junit.framework.*;
import junit.textui.TestRunner;

/**
 * Test {@link org.scoja.util.Substitution}.
 */
public class SubstitutionTest extends TestCase {

    public static void main(final String args[]) {
        TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(SubstitutionTest.class);
    }
    
    public SubstitutionTest(final String name) {
        super(name);
    }
    
    protected void setUp() {
    }

    protected void tearDown() {
    }

    /**
     * Check {@link org.scoja.util.Substitution} parsing.
     */
    public void testParsing()
    throws Exception {
        assertEquals(
            new Substitution(new String[] {"a", "b\\c", "d"},
                             new int[] {1, 12}),
            new Substitution("a\\1b\\\\c\\{12}d"));
        assertEquals(
            new Substitution(new String[] {"", "b\\c", "d"},
                             new int[] {1, 12}),
            new Substitution("\\1b\\\\c\\{12}d"));
        assertEquals(
            new Substitution(new String[] {"a", "b\\c", ""},
                             new int[] {1, 12}),
            new Substitution("a\\1b\\\\c\\{12}"));
    }
    
    /**
     * Check {@link org.scoja.util.Substitution} application to the result
     * of a pattern matching.
     */
    public  void testApplication()
    throws Exception {
        Matcher m = Pattern.compile("([a-z]+),([0-9]+)").matcher("abc,012.");
        assertTrue(m.find());
        assertEquals(
            "|abc|012|", new Substitution("|\\1|\\{2}|").apply(m));
    }
}
