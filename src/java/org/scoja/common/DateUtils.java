/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003  Mario Martínez
 * Copyright (C) 2012  LogTrust
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

import java.io.IOException;
import java.util.Calendar;

/**
 * This a utility class to work with Syslog dates.
 * <p>
 * 
 */
public abstract class DateUtils {

    public static final String[] shortMonthNames = {
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec",
    };
    
    public static final byte[][] shortMonthBytes;
    static {
        shortMonthBytes = new byte[shortMonthNames.length][];
        for (int i = 0; i < shortMonthNames.length; i++) {
            shortMonthBytes[i] = shortMonthNames[i].getBytes();
        }
    }
    
    public static final String[] shortWeekDayNames = {
        "Sun",
        "Mon",
        "Tue",
        "Wed",
        "Thu",
        "Fri",
        "Sat",
    };
    
    public static final String[] longMonthNames = {
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December",
    };
    
    public static final String[] longWeekDayNames = {
        "Sunday",
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
    };
    
    public static final String[] numbers = {
        " 0", " 1", " 2", " 3", " 4", " 5", " 6", " 7", " 8", " 9",
        "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
        "20", "21", "22", "23", "24", "25", "26", "27", "28", "29",
        "30", "31", "32", "33", "34", "35", "36", "37", "38", "39",
        "40", "41", "42", "43", "44", "45", "46", "47", "48", "49",
        "50", "51", "52", "53", "54", "55", "56", "57", "58", "59",
        "60", "61", "62", "63", "64", "65", "66", "67", "68", "69",
        "70", "71", "72", "73", "74", "75", "76", "77", "78", "79",
        "80", "81", "82", "83", "84", "85", "86", "87", "88", "89",
        "90", "91", "92", "93", "94", "95", "96", "97", "98", "99",
    };
    
    public static final String[] numbers0 = {
        "00", "01", "02", "03", "04", "05", "06", "07", "08", "09",
        "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
        "20", "21", "22", "23", "24", "25", "26", "27", "28", "29",
        "30", "31", "32", "33", "34", "35", "36", "37", "38", "39",
        "40", "41", "42", "43", "44", "45", "46", "47", "48", "49",
        "50", "51", "52", "53", "54", "55", "56", "57", "58", "59",
        "60", "61", "62", "63", "64", "65", "66", "67", "68", "69",
        "70", "71", "72", "73", "74", "75", "76", "77", "78", "79",
        "80", "81", "82", "83", "84", "85", "86", "87", "88", "89",
        "90", "91", "92", "93", "94", "95", "96", "97", "98", "99",
    };
    
    public static char digit(final int n) {
        return (char)('0' + n);
    }
    
    public static void print2Digits(final Appendable out, final int n)
    throws IOException {
        out.append(numbers0[n]);
    }
    
    public static void append2Digits0(final byte[] buffer, final int off, 
                                      final int n) {
        buffer[off] = (byte)digit(n/10);
        buffer[off+1] = (byte)digit(n%10);
    }
    
    public static void append2Digits(final byte[] buffer, final int off,
                                     final int n) {
        buffer[off] = (byte)((n < 10) ? ' ' : ('0' + (n/10)));
        buffer[off+1] = (byte)digit(n%10);
    }
    
    public static void append2Digits0(final Appendable sb, final int n)
    throws IOException {
        sb.append(numbers0[n]);
    }
    
    public static void append3Digits0(final Appendable sb, final int n)
    throws IOException {
        sb.append(digit(n/100));
        sb.append(digit((n/10)%10));
        sb.append(digit(n%10));
    }
    
    public static void append4Digits0(final Appendable sb, final int n)
    throws IOException {
        sb.append(digit(n/1000));
        sb.append(digit((n/100)%10));
        sb.append(digit((n/10)%10));
        sb.append(digit(n%10));
    }
    
    protected static int gmtMinutes(final int gmt) {
        return gmt/(60*1000);
    }
    
    public static void printGeneralTZ(final Appendable out, final int gmt)
    throws IOException {
        int n = gmtMinutes(gmt);
        out.append("GMT");
        if (n < 0) { out.append('-'); n = -n; }
        else out.append('+');
        append2Digits0(out, n/60);
        out.append(':');
        append2Digits0(out, n%60);
    }
    
    public static void printRFC822TZ(final Appendable out, final int gmt)
    throws IOException {
        int n = gmtMinutes(gmt);
        if (n < 0) { out.append('-'); n = -n; }
        else out.append('+');
        append2Digits0(out, n/60);
        append2Digits0(out, n%60);
    }
    
    
    /**
     * Format
     * <code>Mon dD hh:mm:ss</code>.
     */
    public static void formatStd(final Appendable out, final Calendar date)
    throws IOException {
        out.append(shortMonthNames[date.get(Calendar.MONTH)]);
        out.append(' ');
        out.append(numbers[date.get(Calendar.DAY_OF_MONTH)]);
        out.append(' ');
        out.append(numbers0[date.get(Calendar.HOUR_OF_DAY)]);
        out.append(':');
        out.append(numbers0[date.get(Calendar.MINUTE)]);
        out.append(':');
        out.append(numbers0[date.get(Calendar.SECOND)]);
    }
    
    /**
     * Format
     * <code>yyyy-mm-dd hh:mm:ss</code>
     */
    public static void formatAbbreviated(final Appendable out,
                                         final Calendar date)
    throws IOException {
        append4Digits0(out, date.get(Calendar.YEAR));
        out.append('-');
        out.append(numbers0[date.get(Calendar.MONTH)+1]);
        out.append('-');
        out.append(numbers0[date.get(Calendar.DAY_OF_MONTH)]);
        out.append(' ');
        out.append(numbers0[date.get(Calendar.HOUR_OF_DAY)]);
        out.append(':');
        out.append(numbers0[date.get(Calendar.MINUTE)]);
        out.append(':');
        out.append(numbers0[date.get(Calendar.SECOND)]);
    }
    
    /**
     * Format
     * <code>yyyy-mm-dd hh:mm:ss.mmm
     */
    public static void formatLong(final Appendable out,
                                  final Calendar date)
    throws IOException {
        formatAbbreviated(out, date);
        out.append('.');
        append3Digits0(out, date.get(Calendar.MILLISECOND));
    }
    
    public static final int WITHGMT_SIZE = 
        4+1+2+1+2 +1+ 2+1+2+1+2+1+3 +1+ 3+1+2+1+2;
    
    /**
     * Format
     * <code>yyyy-mm-dd hh:mm:ss.mmm GMT+hh:mm
     */
    public static void formatGMT(final Appendable out,
                                 final Calendar date)
    throws IOException {
        formatLong(out, date);
        int gmt = date.get(Calendar.ZONE_OFFSET)
            + date.get(Calendar.DST_OFFSET);
        out.append(' ');
        printGeneralTZ(out, gmt);
    }
    
    public static String formatGMT(final Calendar date) {
        final StringBuilder sb = new StringBuilder(WITHGMT_SIZE);
        try {
            formatGMT(sb, date);
        } catch (IOException cannotHappen) {}
        return sb.toString(); 
    }
    
    
    public static Calendar parse(final String date) {
        return parse(date, 0);
    }
    
    public static Calendar parse(final String data, final int init) { 
        final int month = parseMonth(data, init);
        if (month == -1 || data.charAt(init+3) != ' ') return null;
        final int day = parseSpaceInt(data, init+4);
        if (day == -1 || data.charAt(init+6) != ' ') return null;
        final int hour = parseInt2(data, init+7);
        if (hour == -1 || data.charAt(init+9) != ':') return null;
        final int minute = parseInt2(data, init+10);
        if (minute == -1 || data.charAt(init+12) != ':') return null;
        final int second = parseInt2(data, init+13);
        if (second == -1) return null;
        final Calendar date = Calendar.getInstance();
        date.set(Calendar.MONTH, month);
        date.set(Calendar.DAY_OF_MONTH, day);
        date.set(Calendar.HOUR_OF_DAY, hour);
        date.set(Calendar.MINUTE, minute);
        date.set(Calendar.SECOND, second);
        date.set(Calendar.MILLISECOND, 0);
        return date;
    }

    public static int parseMonth(final String data, final int init) {
	int month = -1;
	switch(data.charAt(init)) {
	case 'A':
	    if (data.charAt(init+1) == 'p'
                && data.charAt(init+2) == 'r') month = 3;
	    else if (data.charAt(init+1) == 'u'
                     && data.charAt(init+2) == 'g') month = 7;
	    break;
	case 'D':
	    if (data.charAt(init+1) == 'e'
                && data.charAt(init+2) == 'c') month = 11;
	    break;
	case 'F':
	    if (data.charAt(init+1) == 'e'
                && data.charAt(init+2) == 'b') month = 1;
	    break;
	case 'J':
	    if (data.charAt(init+1) == 'a'
                && data.charAt(init+2) == 'n') month = 0;
	    else if (data.charAt(init+1) == 'u') {
		if (data.charAt(init+2) == 'l') month = 6;
		else if (data.charAt(init+2) == 'n') month = 5;
	    }
	    break;
	case 'M':
	    if (data.charAt(init+1) == 'a') {
		if (data.charAt(init+2) == 'r') month = 2;
		else if (data.charAt(init+2) == 'y') month = 4;
	    }
	    break;
	case 'N':
	    if (data.charAt(init+1) == 'o'
                && data.charAt(init+2) == 'v') month = 10;
	    break;
	case 'O':
	    if (data.charAt(init+1) == 'c'
                && data.charAt(init+2) == 't') month = 9;
	    break;
	case 'S':
	    if (data.charAt(init+1) == 'e'
                && data.charAt(init+2) == 'p') month = 8;
	    break;
	}
	return month;
    }
    
    public static int parseInt2(final String data, final int i) {
        final char c0 = data.charAt(i);
        final char c1 = data.charAt(i+1);
        if (!Character.isDigit(c0) || !Character.isDigit(c1)) return -1;
        return 10*(c0-'0') + (c1-'0');
    }
    
    public static int parseSpaceInt(final String data, final int i) {
        final char c0 = data.charAt(i);
        final char c1 = data.charAt(i+1);
        int result = 0;
        if (Character.isDigit(c0)) result = c0-'0';
        if (Character.isDigit(c1)) result = 10*result + (c1-'0');
        return result;
    }
}
