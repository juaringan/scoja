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

package org.scoja.util;

/**
 * To sum up all those errors when using reflection.
 */
public class EntityNotFoundException
    extends Exception {
    
    public EntityNotFoundException() {
        super();
    }
    
    public EntityNotFoundException(final String message) {
        super(message);
    }
    
    public EntityNotFoundException(final Throwable cause) {
        super(cause);
    }
    
    public EntityNotFoundException(final String message, 
                                   final Throwable cause) {
        super(message, cause);
    }
}
