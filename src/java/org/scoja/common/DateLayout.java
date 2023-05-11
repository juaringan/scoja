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

package org.scoja.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public interface DateLayout
    extends Cloneable {

    //public String format(Date date);

    public Date parse(String date)
    throws ParseException;
    
    public int formatTo(byte[] buffer, int off, Calendar cal);
    
    public String toPattern();
    
    public Object clone();
    
    //======================================================================
    public static class Syslog
        implements DateLayout {
        
        public static final String PATTERN = "MMM dd HH:mm:ss";
        public static final String EXAMPLE = "Dec 12 18:28:59";
            /*                                012345678901234 */
        public static final int WIDTH = PATTERN.length();
        
        private static final Syslog instance = new Syslog();
        
        public static Syslog getInstance() {
            return instance;
        }

        public Date parse(final String dateStr)
        throws ParseException {
            final Calendar cal = DateUtils.parse(dateStr);
            if (cal == null) {
                throw new ParseException("Illegal date " + dateStr, 0);
            }
            return cal.getTime();
        }
        
        public int formatTo(final byte[] buffer, final int off, 
                            final Calendar date) {
            if (buffer.length < off+WIDTH) {
                throw new IllegalArgumentException(
                    "Not enough capacity to format a standard syslog date");
            }
            final byte[] month
                = DateUtils.shortMonthBytes[date.get(Calendar.MONTH)];
            System.arraycopy(month,0, buffer,off,month.length);
            buffer[off+3] = (byte)' ';
            DateUtils.append2Digits(
                buffer, off+4, date.get(Calendar.DAY_OF_MONTH));
            buffer[off+6] = (byte)' ';
            DateUtils.append2Digits0(
                buffer, off+7, date.get(Calendar.HOUR_OF_DAY));
            buffer[off+9] = (byte)':';
            DateUtils.append2Digits0(
                buffer, off+10, date.get(Calendar.MINUTE));
            buffer[off+12] = (byte)':';
            DateUtils.append2Digits0(
                buffer, off+13, date.get(Calendar.SECOND));
            return WIDTH;
        }
        
        public String toPattern() {
            return PATTERN;
        }
        
        public Object clone() {
            return this;
        }
    }
    
    
    //======================================================================
    public static class JDK
        implements DateLayout {
        
        protected final SimpleDateFormat format;
        
        public JDK(final SimpleDateFormat format) {
            this.format = format;
        }
        
        public Date parse(final String dateStr)
        throws ParseException {
            return format.parse(dateStr);
        }
        
        public int formatTo(final byte[] buffer, final int off, 
                            final Calendar cal) {
            final byte[] date = format.format(cal.getTime()).getBytes();
            if (buffer.length < off+date.length) {
                throw new IllegalArgumentException(
                    "Not enough capacity to format a date " + toPattern());
            }
            System.arraycopy(date,0, buffer,off,date.length);
            return date.length;
        }
        
        public String toPattern() {
            return format.toPattern();
        }
        
        public Object clone() {
            return new JDK((SimpleDateFormat)format.clone());
        }
    }
}
