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
 * A test to avoid reading documentation.
 * Finally block is always executed:
 *   when try block ends normally,
 *   when try block ends with an exception,
 *   even if threre is no catching block,
 *   or its catching block ends normally
 *   or with an exception.
 */
public class Finally {
    public static void main(final String[] args) {
        try {
            System.out.println("At try");
            throw new Exception();
        } catch (Exception e) {
            System.out.println("At catch");
            throw new RuntimeException(e);
        } finally {
            System.out.println("At finally");
        }
    }
}
