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

package org.scoja.common;

/**
 * This a utility class to work with Syslog options.
 * Contains named constants for options.
 */
public abstract class Options {

    //======================================================================
    // OPTIONS
    
    /** Log the pid with each message */
    public static final int PID	   = 0x01;
    /** Log on the console if errors in sending */
    public static final int CONS   = 0x02;
    /** Delay open until first syslog() (default) */
    public static final int ODELAY = 0x04;
    /** Don't delay open */
    public static final int NDELAY = 0x08;
    /** Don't wait for console forks: DEPRECATED */
    public static final int NOWAIT = 0x10;
    /** Log to stderr as well */
    public static final int PERROR = 0x20;
    
}
