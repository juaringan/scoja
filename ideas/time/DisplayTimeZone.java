// $Id: DisplayTimeZone.java,v 1.1 2003/08/25 10:47:28 elmartinfierro Exp $
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

import java.util.Calendar;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
 
public class DisplayTimeZone {

    public static void main(final String[] args) throws Exception {
        showTimeZone(Calendar.getInstance());
        final SimpleDateFormat df
            = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < args.length; i++) {
            final Calendar cal = Calendar.getInstance();
            cal.setTime(df.parse(args[i]));
            showTimeZone(cal);
        }
    }
    
    public static void showTimeZone(final Calendar cal) {
        final TimeZone tz = cal.getTimeZone();
        System.out.print
            (cal.getTime()
             + "\n  Time zone: " + tz
             + "\n  Time zone display name: " + tz.getDisplayName()
             + "\n  Time zone short display name: "
             +   tz.getDisplayName(cal.get(Calendar.DST_OFFSET) > 0,
                                   TimeZone.SHORT)
             + "\n  Time zone id: " + tz.getID()
             + "\n");
    }
}
