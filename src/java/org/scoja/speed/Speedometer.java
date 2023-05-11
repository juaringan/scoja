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
package org.scoja.speed;

import org.scoja.cc.lang.Structural;

public class Speedometer {

    protected final long minInterval;
    protected final int[] intervalMultiples;
    protected final long[] countPerInterval;
    protected final long[] sizePerInterval;
    protected final long[] countPerBucket;
    protected final long[] sizePerBucket;
    protected long start;
    protected long currentEnd;
    protected int next;
    protected long currentCount;
    protected long currentSize;

    public Speedometer(final int[] intervalSizes) {
        final int n = intervalSizes.length;
        this.minInterval = intervalSizes[0];
        this.intervalMultiples = new int[n];
        this.countPerInterval = new long[n+1];
        this.sizePerInterval = new long[n+1];
        final int round = intervalSizes[0] / 2;
        for (int i = 0; i < n; i++) {
            this.intervalMultiples[i]
                = (intervalSizes[i] + round)/intervalSizes[0];
        }
        this.sizePerBucket = new long[this.intervalMultiples[n-1]];
        this.countPerBucket = new long[this.intervalMultiples[n-1]];
        this.start = this.currentEnd = 0;
        this.next = 0;
        this.currentCount = 0;
        this.currentSize = 0;
    }
    
    public int intervals() {
        return sizePerInterval.length;
    }
    
    public synchronized void consider(final int n) {
        update();
        if (n >= 0) { currentCount++; currentSize += n; }
    }
    
    public synchronized void performance(final double[] cps,
            final double[] sps) {
        update();
        double secs;
        final int n = intervalMultiples.length;
        for (int i = 0; i < n; i++) {
            secs = minInterval * intervalMultiples[i] / 1000.0;
            cps[i] = countPerInterval[i] / secs;
            sps[i] = sizePerInterval[i] / secs;
        }
        secs = (currentEnd - start - minInterval) / 1000.0;
        cps[n] = countPerInterval[n] / secs;
        sps[n] = sizePerInterval[n] / secs;
    }
    
    protected void update() {
        final long now = now();
        if (now < currentEnd) return;
        if (start == 0) { 
            start = now; 
            currentEnd = now + minInterval;
            return;
        }
        do {
            for (int i = 0; i < intervalMultiples.length; i++) {
                final int n 
                    = (next + intervalMultiples[i]) % sizePerBucket.length;
                countPerInterval[i] += currentCount - countPerBucket[n];
                sizePerInterval[i] += currentSize - sizePerBucket[n];
            }
            countPerInterval[countPerInterval.length-1] += currentCount;
            sizePerInterval[sizePerInterval.length-1] += currentSize;
            countPerBucket[next] = currentCount;
            sizePerBucket[next] = currentSize;
            currentEnd += minInterval;
            currentCount = 0;
            currentSize = 0;
            if (next == 0) next = sizePerBucket.length - 1;
            else next--;
        } while (currentEnd <= now);
    }
    
    protected long now() {
        return System.currentTimeMillis();
    }
        
    public String toString() {
        final double[] cps = new double[sizePerInterval.length];
        final double[] sps = new double[sizePerInterval.length];
        performance(cps, sps);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sizePerInterval.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(i).append(':').append(sps[i])
                .append('/').append(cps[i])
                .append('=').append(sps[i]/cps[i]);
        }
        return sb.toString();
    }
}
