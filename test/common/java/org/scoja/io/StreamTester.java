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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamTester {

    protected final InputStream is;
    protected final OutputStream os; 

    protected final DataProvider readDP;
    protected final DataProvider writeDP;
        
    protected final Thread reader;
    protected final Thread writer;
    protected long received;
    protected long send;
    protected boolean stopRequested;
    protected BadTransferException error;

    public StreamTester(final DataProvider dp,
                        final InputStream is,
                        final OutputStream os) {
        this.is = is;
        this.os = os;
        
        this.readDP = dp;
        this.writeDP = (DataProvider)dp.clone();
        
        this.reader = new Thread() {
                public void run() { read(); }
            };
        this.writer = new Thread() {
                public void run() { write(); }
            };
        this.received = this.send = 0;
        this.stopRequested = false;
        this.error = null;
    }

    public void start() {
        reader.start();
        writer.start();
    }
    
    public synchronized void shouldStop() {
        stopRequested = true;
    }
    
    protected synchronized boolean stopRequested() {
        return stopRequested;
    }
    
    protected synchronized void setError(final BadTransferException error) {
        if (this.error == null) this.error = error;
    }
    
    public void join()
    throws InterruptedException, BadTransferException {
        reader.join();
        writer.join();
        synchronized (this) {
            if (error != null) throw error;
        }
        if (received != send) {
            throw new BadTransferException(
                send + " bytes send, but " + received + " received");
        }
    }
    
    public long transferred() {
        return send;
    }
    
    protected void read() {
        byte[] expected = readDP.next();
        int nextExpected = 0;
        final byte[] buffer = new byte[1024];
        int nextBuffer = 0, bufferSize = 0;
        try {
            mainLoop:
            for (;;) {
                if (nextExpected == expected.length) {
                    expected = readDP.next();
                    nextExpected = 0;
                } else if (nextBuffer == bufferSize) {
                    bufferSize = is.read(buffer);
                    if (bufferSize == -1) break mainLoop;
                    received += bufferSize;
                    nextBuffer = 0;
                } else {
                    final int toCmp = Math.min(expected.length - nextExpected,
                                               bufferSize - nextBuffer);
                    for (int i = 0; i < toCmp; i++) {
                        if (buffer[nextBuffer+i] != expected[nextExpected+i]) {
                            setError(new BadTransferException(
                                         "A " + expected[nextExpected+i]
                                         + " expected"
                                         + ", but a " + buffer[nextBuffer+i]
                                         + " received"));
                            break mainLoop;
                        }
                    }
                    nextExpected += toCmp;
                    nextBuffer += toCmp;
                }
            }
            is.close();
        } catch (IOException e) {
            setError(new BadTransferException(e));
        }
    }
    
    protected void write() {
        try {
            while (!stopRequested()) {
                final byte[] data = writeDP.next();
                os.write(data);
                send += data.length;
            }
            os.close();
        } catch (IOException e) {
            setError(new BadTransferException(e));
        }
    }
}
