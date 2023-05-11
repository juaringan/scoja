/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2008  Bankinter, S.A.
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

/**
 *
 * <b>The way to use this interface.</b>
 * Suppose that our buffer is
 * <pre><tt>
 * byte[] buffer;
 * </tt></pre>
 *
 * <p>
 * To add data in
 * <pre><tt>
 * byte[] toadd;
 * </tt></pre>
 * respect the following template:
 * <pre><tt>
 * if (buffer.length - end < toadd.length) {
 *     final int newlen
 *          = resizer.newLength(buffer.length, init, end, toadd.length);
 *     buffer = resizer.resized(
 *         buffer, init, end, Math.max(buffer.length,newlen));
 *     end -= init;
 *     init = 0;
 * }
 * if (buffer.length - end < toadd.length) {
 *     //Error: not enough space
 * }
 * System.arracopy(toadd,0, buffer,end,toadd.length);
 * end += toadd.length;
 * </tt></pre>
 *
 * <p>
 * To remove <tt>removed</tt> bytes 
 * <pre><tt>
 * init -= removed;
 * final int newlen = newLength(buffer.length, init, end, 0);
 * if (newlen < buffer.length) {
 *     buffer = resizer.resized(buffer, init, end, newlen);
 * }
 * </tt></pre>
 */
public interface BufferResizer {

    public void setSpaceControl(SpaceController cache);

    public int newLength(int len, int init, int end, int capacity);
    
    public int newLength(byte[] buffer, int init, int end, int capacity);
    
    public byte[] resized(byte[] buffer, int init, int end, int newlen);
        
        
    //======================================================================
    public static abstract class Skeleton implements BufferResizer {
        
        public int newLength(final byte[] buffer,
                             final int init, final int end,
                             final int capacity) {
            return newLength(buffer == null ? 0 : buffer.length,
                             init, end, capacity);
        }
    }

        
    //======================================================================
    public static class Multiplicative
        extends Skeleton
        implements BufferResizer {
        
        protected SpaceController cache;
        protected int minSize;
        protected float copyRatio;
        protected float growRatio;
        protected float usageRatio;
        
        public Multiplicative() {
            this(0);
        }
        
        public Multiplicative(final int minSize) {
            this.cache = new SpaceController.Unlimited();
            this.minSize = minSize;
            this.copyRatio = 3f/2;
            this.growRatio = 2;
            this.usageRatio = 4;
        }
        
        public void setSpaceControl(final SpaceController cache) {
            this.cache = cache;
        }
        
        public void setMinSize(final int minSize) {
            this.minSize = minSize;
        }
        
        public void setCopyRation(final float copyRatio) {
            this.copyRatio = copyRatio;
        }
    
        public void setGrowRatio(final float growRatio) {
            this.growRatio = growRatio;
        }

        public void setUsageRatio(final float usageRatio) {
            this.usageRatio = usageRatio;
        }
        
        public int newLength(final int len, final int init, final int end,
                             final int capacity) {
            final int used = end - init;
            if (capacity == 0) {
                if (used*usageRatio < len) {
                    return Math.max(minSize, (int)Math.ceil(used*growRatio));
                }
            } else {
                final int free = len - used;
                if (capacity > free || used*copyRatio > len) {
                    return Math.max((int)Math.ceil(used*growRatio),
                                    used+capacity);
                }
            }
            return len;
        }
    
        public byte[] resized(final byte[] buffer,
                              final int init, final int end, final int newlen){
            final int curlen = (buffer == null) ? 0 : buffer.length;
            final int reallen = cache.resizing(curlen, newlen);
            if (reallen != curlen) {
                if (newlen == 0) return null;
                try {
                    final byte[] newbuffer = new byte[reallen];
                    if (buffer != null) {
                        System.arraycopy(buffer,init, newbuffer,0,end-init);
                    }
                    return newbuffer;
                } catch (OutOfMemoryError e) {
                    cache.resizeFailed(reallen, curlen);
                }
            }
            if (init > 0 && buffer != null) {
                System.arraycopy(buffer,init, buffer,0,end-init);
            }
            return buffer;
        }
    }
}
