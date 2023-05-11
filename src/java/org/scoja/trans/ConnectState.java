/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2010  Mario Martínez
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

import java.io.IOException;

/**
 * These are the states that a TransportLine pass through.
 * A TransportLine is created 
 * either {@link #UNCONNECTED} ({@link Transport#newClient()})
 * or {@link #CONNECTING} ({@link TransportService#accept()}).
 * Then passes to {@link #CONNECTED} (if connection step is done successfully)
 * or to {@link #DISCONNECTED} (if something fails).
 * The first error on a {@link #CONNECTING} or {@link #CONNECTED} line changes
 * the state to {@link #DISCONNECTED}, even if the line is used indirectly
 * with its {@link IStream} or {@link OStream}.
 * Finally, a close() call changes its state to {@link #CLOSED};
 * this is a final state for a TransportLine returned from
 * a normal {@link Transport},
 * either directly with {@link Transport#newClient}
 * or indirectly with {@link TransportService#accept}.
 *
 * <p>
 * The end of the input doesn't directly means a state change.
 * But if this happens while {@link #CONNECTING} an error is raised
 * (because the handshake could not complete) and the state is changed to
 * {@link DISCONNECTED}.
 * <p>
 * The end of output (cannot write because the peer closed its line)
 * usually produces an exception and the state is changed to
 * {@link DISCONNECTED}.
 * The following reads will return a -1.
 */
public enum ConnectState {
    UNCONNECTED, CONNECTING, CONNECTED, DISCONNECTED, CLOSED;
    
    public boolean open() {
        return open(this);
    }
    
    public static boolean open(final ConnectState state) {
        return state == CONNECTING || state == CONNECTED;
    }
    
    public boolean connectDone() {
        return connectDone(this);
    }
    
    public static boolean connectDone(final ConnectState state) {
        return CONNECTING.compareTo(state) < 0;
    }
    
    public static void checkNotClosed(final ConnectState state)
    throws IOException {
        if (state == UNCONNECTED) throw new IOException("Closed");
    }
}
