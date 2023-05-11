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

package org.scoja.io;

import junit.framework.*;
import junit.textui.TestRunner;

/**
 * This is an stress test for pipes.
 */
public class PipeStressTest
    extends TestCase {

    public static void main(final String args[]) {
        TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(PipeStressTest.class);
    }
    
    protected long duration;
    
    public PipeStressTest(final String name) {
        super(name);
    }
    
    protected void setUp() {
        this.duration = org.scoja.StressConfiguration.getDuration();
    }

    protected void tearDown() {
    }

    public void testTransfer() throws Exception {
        final DataStorage ds = new ExhaustiveDataStorage(100, 10, 10000);
        final DataProvider dp = new DataProvider(ds);
        final Pipe pipe = new Pipe();
        
        final StreamTester st
            = new StreamTester(dp, pipe.getSource(), pipe.getSink());
        st.start();
        Thread.sleep(duration);
        st.shouldStop();
        st.join();
        System.out.println("Bytes transferred: " + st.transferred());
    }
}
