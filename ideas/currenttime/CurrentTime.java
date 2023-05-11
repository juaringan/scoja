/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
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

/**
 * Test how fast is {@link System#currentTimeMillis()}.
 * It is pretty fast:
 * my current laptop (Intel(R) Core(TM)2 CPU T7400 @ 2.16GHz)
 * performs around 800000 <tt>currentTimeMillis</tt> per second.
 */
public class CurrentTime {

    public static void main(final String[] args) {
        final int n = Integer.parseInt(args[0]);
        for (;;) {
            final long total = getTime(n);
            System.out.print(
                "\r" + (int)(n/(total/1000.0)) + " ops per second          ");
        }
    }
    
    private static long getTime(final int n) {
        long init = System.currentTimeMillis();
        long end = 0;
        for (int i = 0; i < n; i++) {
            end = System.currentTimeMillis();
        }
        return end - init;
    }
}
