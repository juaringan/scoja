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

package org.scoja.server.cache;

import java.util.ArrayList;
import junit.framework.*;
import junit.textui.TestRunner;

/**
 */
public class BufferResizerTest extends TestCase {

    public static void main(final String args[]) {
        TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(BufferResizerTest.class);
    }
    
    public BufferResizerTest(final String name) {
        super(name);
    }
    
    protected void setUp() throws Exception {
    }

    protected void tearDown() {
    }

    public void testResized() {
        final Case[] cases = {
            new Case(new BufferResizer.Multiplicative())
            .add(0, 0, 0, 20, true)
            .add(100, 0, 20, 20, false)
            .add(100, 80, 90, 20, false)
            .add(100, 10, 90, 20, true)
            .add(100, 10, 90, 0, false)
            .add(100, 10, 20, 0, true)
            .add(100, 20, 20, 0, true),
        };
        for (final Case c: cases) {
            for (final Behaviour bh: c.behaviours) {
                checkBehaviour(c.resizer, bh);
            }
        }
    }
    
    protected void checkBehaviour(final BufferResizer br,
                                  final Behaviour bh) {
        final int newlen = br.newLength(bh.len, bh.init, bh.end, bh.capacity);
        final byte[] buffer0 = new byte[bh.len];
        for (int i = 0; i < bh.end - bh.init; i++) {
            buffer0[bh.init + i] = (byte)i;
        }
        final byte[] buffer1
            = br.resized(buffer0, bh.init, bh.end, newlen);
        System.err.println(
            "Resizing: " + bh.len + "[" + bh.init + "," + bh.end
            + ") + " + bh.capacity + " (" + bh.reallocated + ") -> "
            + newlen + ":"
            + ((buffer1 == null) ? "null" : Integer.toString(buffer1.length)));
        assertEquals(bh.reallocated, buffer0 != buffer1);
        for (int i = 0; i < bh.end - bh.init; i++) {
            assertEquals((byte)i, buffer1[i]);
        }
    }

    protected static class Case {
        protected final BufferResizer resizer;
        protected final ArrayList<Behaviour> behaviours;
        
        public Case(final BufferResizer resizer) {
            this.resizer = resizer;
            this.behaviours = new ArrayList<Behaviour>();
        }
        
        public Case add(final int len, final int init, final int end,
                        final int capacity, final boolean reallocated) {
            behaviours.add(new Behaviour(len,init,end,capacity,reallocated));
            return this;
        }
    }
    
    protected static class Behaviour {
        protected final int len, init, end, capacity;
        protected final boolean reallocated;
        
        public Behaviour(final int len, final int init, final int end,
                         final int capacity, final boolean reallocated) {
            this.len = len;
            this.init = init;
            this.end = end;
            this.capacity = capacity;
            this.reallocated = reallocated;
        }
    }
}
