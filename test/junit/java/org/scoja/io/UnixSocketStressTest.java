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

import java.io.File;
import java.io.IOException;

import junit.framework.*;
import junit.textui.TestRunner;

/**
 * This is an stress test for Unix domain sockets.
 */
public class UnixSocketStressTest
    extends TestCase {

    public static void main(final String args[]) {
        TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(UnixSocketStressTest.class);
    }
    
    protected long duration;
    
    public UnixSocketStressTest(final String name) {
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
        final File file = File.createTempFile("scoja", ".stream-socket");
        final UnixSocketAddress sa = new UnixSocketAddress(file);
        final UnixSocket client = new UnixSocket();
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(100);
                    client.connect(sa);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
        
        final UnixServerSocket server = new UnixServerSocket();
        sa.clear();
        System.out.println("Binding to " + sa);
        server.bind(sa);
        final UnixSocket slave = server.accept();
        
        final StreamTester st1 = new StreamTester(
            dp, slave.getInputStream(), client.getOutputStream());
        final StreamTester st2 = new StreamTester(
            (DataProvider)dp.clone(), 
            client.getInputStream(), slave.getOutputStream());
        st1.start();
        st2.start();
        Thread.sleep(duration);
        st1.shouldStop();
        st2.shouldStop();
        st1.join(); 
        st2.join();
        System.out.println("Bytes transferred from server to client: "
                           + st1.transferred());
        System.out.println("Bytes transferred from client to server: "
                           + st2.transferred());
                           
        sa.clear();
    }
}
