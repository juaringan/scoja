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

import java.nio.ByteBuffer;

import junit.framework.*;
import junit.textui.TestRunner;

import org.scoja.cc.io.Charsets;
import org.scoja.trans.OStream;

public class ByteFallTest extends TestCase {

    public static void main(final String args[]) {
        TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(ByteFallTest.class);
    }
    
    public ByteFallTest(final String name) {
        super(name);
    }
    
    protected void setUp() {
    }

    protected void tearDown() {
    }

    public void testMemory()
    throws Exception {
        final MyOStream os = new MyOStream();
        final byte[] data3 = Charsets.latin1("123");
        final ByteFall bf = new MemoryByteFall(16);
        System.err.println(bf);
        assertEquals(0, bf.size());
        for (int i = 0; i < 16/3; i++) {
            bf.add(data3, 0, 3);
            System.err.println(bf);
            assertEquals((i+1)*3, bf.size());
        }
        for (int i = 0; i <= 16; i++) {
            bf.add(data3, 0, 3);
            System.err.println(bf);
            assertEquals(15, bf.size());
        }
        os.n = 3; bf.unload(os);
        System.err.println(bf);
        assertEquals(12, bf.size());
        os.n = 4; bf.unload(os);
        System.err.println(bf);
        assertEquals(8, bf.size());
        for (int i = 0; i < 4 ; i++) {
            bf.add(data3, 0, 3);
            System.err.println(bf);
        }
        os.n = 3; bf.unload(os);
        System.err.println(bf);
        assertEquals(12, bf.size());
        os.n = 4; bf.unload(os);
        System.err.println(bf);
        assertEquals(8, bf.size());
        bf.dropPartial();
        System.err.println(bf);
        assertEquals(6, bf.size());
        
        os.n = 100; bf.unload(os);
        System.err.println(bf);
        assertEquals(0, bf.size());
    }
    
    protected static class MyOStream implements OStream {
        public int n;
        
        public int write(byte[] bs, int off, int len) {
            final int m = write(len);
            System.err.print("DATA: ");
            System.err.write(bs, off, m);
            System.err.println();
            return m;
        }
    
        public int write(ByteBuffer bs) {
            return write(bs.remaining());
        }

        protected int write(int len) {
            int m = Math.min(n, len);
            n -= m;
            return m;
        }
    
        public int flush() { return 0; }
    }
}
