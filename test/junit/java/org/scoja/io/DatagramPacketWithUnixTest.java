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

import java.net.DatagramPacket;

/**
 * This test is currently useless.
 * We hoped to use {@link java.net.DatagramPacket}
 * with {@link org.scoja.io.UnixSocketAddress}
 * (a subclass of {@link java.net.SocketAddress}).
 * But unfortunately, {@link java.net.DatagramPacket} seems to work only
 * with {@link java.net.InetSocketAddress} and with no other subclass
 * of {@link java.net.SocketAddress}.
 */
public class DatagramPacketWithUnixTest extends TestCase {

    public static void main(final String args[]) {
        TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(DatagramPacketWithUnixTest.class);
    }
    
    public DatagramPacketWithUnixTest(final String name) {
        super(name);
    }
    
    protected void setUp() {
    }

    protected void tearDown() {
    }

    /**
     * Unix sockets suppose that {@link java.net.DatagramPacket} stores a
     * reference to the data array instead of coping it.
     * This method checks that it is so.
     */
    public void testDataReference() throws Exception {
        final byte[] array = new byte[1024];
        final DatagramPacket packet = new DatagramPacket(array, array.length);
        assertSame("Unexpected behaviour:"
                   + " DatagramPacket copies the data array.",
                   array, packet.getData());
    }
    
    /**
     * Unix socket could use {@link java.net.DatagramPacket}
     * with a {@link org.scoja.io.UnixSocketAddress} instead of
     * with a {@link java.net.InetSocketAddres}.
     * This method checks that this is legal usage.
     * <b>It is NOT LEGAL</b>.
     * So Unix domain datagram sockets use
     * {@link java.net.DatagramPacket} no more.
     * A {@link org.scoja.io.GenericDatagramSocket} has been implement
     * that work with every {@link java.net.SocketAddress}.
     */
    /* Because of porting Scoja client to 1.3, UnixSocketAddress is no more
     * a subclass of SocketAddress. So this test has no sense.
    public void testUnixAddress() throws Exception {
        final byte[] array = new byte[1024];
        final UnixSocketAddress address = new UnixSocketAddress("/tmp/socket");
        try {
            final DatagramPacket packet
                = new DatagramPacket(array, array.length, address);
            fail("Unexpected behaviour: "
                 + " DatagramPacket allows UnixSocketAddress.");
        } catch (Exception e) {
        }
    }
    */
}
