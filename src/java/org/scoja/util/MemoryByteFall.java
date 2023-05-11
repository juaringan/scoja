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

package org.scoja.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import org.scoja.trans.OStream;

public class MemoryByteFall implements ByteFall {

    public static final float DEFAULT_MIN_USAGE_RATIO = 0.5f;
    public static final float DEFAULT_GROW_RATIO = 0.5f;

    protected int maxSize;
    protected float minUsageRatio;
    protected float growRatio;
    protected int minUsage;
    
    protected boolean firstIsPartial;
    protected byte[] store;
    protected int storeFirst;
    protected int storeUsed;
    protected int[] sizes;
    protected int sizesFirst;
    protected int sizesUsed;
    
    public MemoryByteFall(final int maxSize) {
        this.maxSize = maxSize;
        this.minUsageRatio = DEFAULT_MIN_USAGE_RATIO;
        this.growRatio = DEFAULT_GROW_RATIO;
        this.minUsage = 0;
        
        this.firstIsPartial = false;
        this.store = null;
        this.storeFirst = this.storeUsed = 0;
        this.sizes = null;
        this.sizesFirst = this.sizesUsed = 0;
    }
    
    public void setGrowRatio(final float growRatio) {
        this.growRatio = growRatio;
    }
    
    public void setMinUsageRatio(final float minUsageRatio) {
        this.minUsageRatio = minUsageRatio;
    }
    
    public boolean isEmpty() {
        return sizesUsed == 0;
    }
    
    public int itemCount() {
        return sizesUsed;
    }
    
    public int size() {
        return storeUsed;
    }
    
    public int add(final byte[] bs, final int off, final int len) {
        return add(false, bs, off, len);
    }
    
    public int add(final boolean partial,
            final byte[] bs, final int off, final int len) {
        if (len == 0) return 0;
        if (isEmpty()) firstIsPartial = partial;
        else if (partial) throw new IllegalStateException(
            "Cannot add a partial block on a non-empty ByteFall");
        final int dropped = ensureSpace(len);
        final int avail = storeAvailable();
        final int len0 = Math.min(len, avail);
        final int strNext = storeNext();
        copyArrayToCircular(bs, off, store, strNext, len0);
        storeUsed += sizes[sizesNext()] = len0;
        sizesUsed++;
        return dropped;
    }
    
    public int dropPartial() {
        if (!firstIsPartial) return 0;
        final int n = dropItems(1);
        recoverSpace();
        return n;
    }
    
    public int unload(final WritableByteChannel out)
    throws IOException {
        if (isEmpty()) return 0;
        final int end = storeNext();
        int n;
        if (storeFirst < end) {
            n = out.write(ByteBuffer.wrap(store, storeFirst, storeUsed));
        } else {
            final int tocopy = store.length - storeFirst;
            n = out.write(ByteBuffer.wrap(store, storeFirst, tocopy));
            if (n == tocopy) n += out.write(ByteBuffer.wrap(store, 0, end));
        }
        dropBytes(n, true);
        recoverSpace();
        return n;
    }
    
    public int unload(final OStream out)
    throws IOException {
        if (isEmpty()) return 0;
        final int end = storeNext();
        int n;
        if (storeFirst < end) {
            n = out.write(store, storeFirst, storeUsed);
        } else {
            final int tocopy = store.length - storeFirst;
            n = out.write(store, storeFirst, tocopy);
            if (n == tocopy) n += out.write(store, 0, end);
        }
        dropBytes(n, true);
        recoverSpace();
        return n;
    }
    
    protected int dropItems(final int i) {
        final int m = storeSpanSize(sizesFirst, i);
        sizesFirst = sizesIdx(sizesFirst + i);
        sizesUsed -= i;
        firstIsPartial = false;
        storeFirst = (storeFirst + m) % store.length;
        storeUsed -= m;
        return m;
    }
    
    protected int dropBytes(final int b, final boolean partialAllowed) {
        if (b <= 0) return 0;
        int m = 0;
        for (;;) {
            final int m0 = m + sizes[sizesFirst];
            if (m0 > b) break;
            m = m0;
            sizesFirst = sizesIdx(sizesFirst + 1);
            sizesUsed--;
        }
        if (m == b) {
            firstIsPartial = false;
        } else /*m < b*/ if (partialAllowed) {
            final int prefix = b - m;
            m = b;
            sizes[sizesFirst] -= prefix;
            firstIsPartial = true;
        } else /*m < b && !partialAllowed*/ {
            m += sizes[sizesFirst];
            sizesFirst = sizesIdx(sizesFirst + 1);
            sizesUsed--;
            firstIsPartial = false;
        }
        storeFirst = (storeFirst + m) % store.length;
        storeUsed -= m;
        return m;
    }
    
    protected int ensureSpace(final int needed) {
        final int avail = (sizes == null) ? 0 : storeAvailable();
        if (avail < needed) {
            final int curSize = (store == null) ? 0 : store.length;
            final int newSize = Math.min(maxSize,
                    Math.max((int)Math.ceil(growRatio*curSize),
                            storeUsed+needed));
            final int overflow = (storeUsed + needed) - newSize;
            if (overflow > 0) {
                dropBytes(Math.min(storeUsed, overflow), false);
            }
            if (newSize > curSize) {
                final byte[] newStore = new byte[newSize];
                if (store != null) {
                    int tocopy = Math.min(curSize-storeFirst, storeUsed);
                    System.arraycopy(store, storeFirst, newStore, 0, tocopy);
                    if (tocopy < storeUsed) {
                        System.arraycopy(store, 0, 
                                newStore, tocopy, storeUsed-tocopy);
                    }
                }
                store = newStore;
                storeFirst = 0;
                minUsage = (int)Math.floor(minUsageRatio*newSize);
            }
        }
        if (sizes == null) {
            sizes = new int[1];
            sizesFirst = sizesUsed = 0;
        } else if (sizesUsed == sizes.length) {
            final int[] newSizes = new int[2*sizes.length];
            final int block1 = sizes.length - sizesFirst;
            System.arraycopy(sizes, sizesFirst, newSizes, 0, block1);
            System.arraycopy(sizes, 0, newSizes, block1, sizesFirst);
            sizes = newSizes;
            sizesFirst = 0;
        }
        return 0;
    }
    
    protected void recoverSpace() {
        if (storeUsed == 0) {
            store = null;
            storeFirst = storeUsed = 0;
            sizes = null;
            sizesFirst = sizesUsed = 0;
            return;
        }
    }
    
    protected void copyArrayToCircular(final byte[] src, final int srcoff,
            final byte[] trg, final int trgoff, final int len) {
        final int block1 = Math.min(len, trg.length - trgoff);
        System.arraycopy(src, srcoff, trg, trgoff, block1);
        if (block1 < len) {
            System.arraycopy(src, srcoff+block1, trg, 0, len-block1);
        }
    }
    
    protected int storeAvailable() {
        return store.length - storeUsed;
    }
    
    protected int storeNext() {
        return (storeFirst + storeUsed) % store.length;
    }
    
    protected int storeSpanSize(final int idx, final int n) {
        int s = 0;
        for (int i = 0; i < n; i++) s += sizes[sizesIdx(idx+i)];
        return s;
    }
    
    protected int sizesNext() {
        return sizesIdx(sizesFirst + sizesUsed);
    }
    
    protected int sizesIdx(final int n) { 
        return n & (sizes.length - 1);
    }
    
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MemoryByteFall[")
            .append("store len: ").append(
                (store == null) ? "null" : Integer.toString(store.length))
            .append(", store first: ").append(storeFirst)
            .append(", store used: ").append(storeUsed)
            .append(", is first partial: ").append(firstIsPartial)
            .append(", sizes len: ").append(
                (sizes == null) ? "null": Integer.toString(sizes.length))
            .append(", sizes first: ").append(sizesFirst)
            .append(", sizes used: ").append(sizesUsed)
            .append(", sizes:");
        if (sizes != null) for (int i = 0; i < sizesUsed; i++) {
            final int idx = sizesIdx(sizesFirst+i);
            sb.append(' ').append(idx).append(':').append(sizes[idx]);
        }
        return sb.append("]").toString();
    }
}
