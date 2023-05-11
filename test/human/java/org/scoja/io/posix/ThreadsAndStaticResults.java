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

package org.scoja.io.posix;

/**
 * This test is sensible only when user or group information is retrieved
 * with thread-unsafe C functions.
 * Currently native Posix implementation use thread-safe functions, so
 * it has no use.
 * But in previous versions, when thread-unsafe functions were used,
 * this test checked that the synchronization at Java level worked properly.
 *
 * <p>
 * This test build serveral threads, each one retrieving endlessly info
 * for the same user. It this info changes, then threre is a concurrent
 * bug.
 * This program never ends, and only prints information when a malfunction
 * is detected.
 */
public class ThreadsAndStaticResults implements Runnable {

    private static final String[] users = {
        "ftp", "mail", "root",
    };

    public static void main(final String[] args) throws Throwable {
        if (args.length != 1) {
            System.err.println(
                "java " + ThreadsAndStaticResults.class.getName()
                + " PosixLike"
                + "\nPosixLike:"
                + "\n    " + PosixFree.class.getName()
                + "\n    " + PosixNative.class.getName());
            System.exit(-1);
        }            
        
        int argc = 0;
        final String posixLike = args[argc++];
        
        Posix.setPosix(posixLike);
        
        for (int i = 0; i < users.length; i++) {
            new Thread(new ThreadsAndStaticResults(users[i])).start();
        }
    }
    
    
    //======================================================================
    protected final String user;
    
    public ThreadsAndStaticResults(final String user) {
        this.user = user;
    }

    public void run() {
        final PosixLike ps = Posix.getPosix();
        final UserInfo first = ps.getUserInfo(user);
        try {
            for (;;) {
                final UserInfo again = ps.getUserInfo(user);
                System.out.println(again);
                if (!first.equals(again)) {
                    System.out.print("First: " + first
                                     + "\nagain: " + again
                                     + "\n");
                }
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }
}
