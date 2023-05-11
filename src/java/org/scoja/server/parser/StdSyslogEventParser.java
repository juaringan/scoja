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

package org.scoja.server.parser;

import java.util.Calendar;

import org.scoja.common.DateLayout;
import org.scoja.common.DateUtils;

public class StdSyslogEventParser implements EventParser {

    private static final StdSyslogEventParser instance
        = new StdSyslogEventParser();
        
    public static StdSyslogEventParser getInstance() {
        return instance;
    }

    public String trim(final byte[] data, final int offset, final int length) {
        int init = offset;
        int end = offset+length;
        while (init < end && (data[end-1] & 0xFF) <= ' ') end--;
        while (init < end && (data[init] & 0xFF) <= ' ') init++;
        final char[] pack = new char[end-init];
        for (int i = 0; i < pack.length; i++) {
            pack[i] = (char)(data[init+i] & 0xFF);
        }
        return new String(pack);
    }
    
    public Calendar parseDate(final String date) {
        return DateUtils.parse(date);
    }
    
    public void parseOn(final ParsedEvent event, final String pack) {
        final int packLength = pack.length();
        int i = 0;
        
        if (packLength <= i || pack.charAt(i) != '<') {
            initDataFailed(event, pack, 0);
            return;
        }
        
        final int priorityEnd = pack.indexOf('>', i);
        if (priorityEnd == -1) {
            initDataFailed(event, pack, 0);
            return;
        }
        
        final int priority = parseInt(pack, i+1, priorityEnd);
        if (priority != -1) event.setPriority(priority);
        i = priorityEnd+1;
        
        if (i >= packLength) {
            initDataFailed(event, pack, i);
            return;
        }
        if (pack.charAt(i) != ' ') {
            final int dateend = i + DateLayout.Syslog.WIDTH;
            if (dateend == packLength 
                || (dateend < packLength && pack.charAt(dateend) == ' ')) {
                event.setDate(pack.substring(i, i + DateLayout.Syslog.WIDTH));
                i += DateLayout.Syslog.WIDTH;
            }
        }
        
        while (i < packLength && pack.charAt(i) == ' ') i++;
        final int hostBegin = i;
        while (i < packLength 
               && pack.charAt(i) != ' ' 
               && pack.charAt(i) != ':') i++;
        int hostEnd, dataBegin;
        if (i >= packLength || pack.charAt(i) == ':') {
            hostEnd = dataBegin = hostBegin;
        } else {
            hostEnd = i;
            event.setHost(pack.substring(hostBegin, hostEnd));
            i++;
            while (i < packLength && pack.charAt(i) == ' ') i++;
            dataBegin = i;
        }
        
        final String data = pack.substring(dataBegin);
        event.setData(data);
        
        //Now we try to find the first occurrence of ":".
        //For a naive search, it is enough:
        //final int appEnd = pack.indexOf(':', i);
        //But to allow program names with ":", we prioritize ": " to ":".
        int appEnd = -1;
        for (int j = i; j < packLength; j++) {
            if (pack.charAt(j) == ':') {
                if (j+1 < packLength && pack.charAt(j+1) == ' ') {
                    appEnd = j;
                    break;
                } else if (appEnd == -1) {
                    appEnd = j;
                }
            }
        }
        
        if (appEnd == -1) {
            event.setMessage(data);
            return;
        }
        
        int messInit = appEnd+1;
        if (messInit < packLength && pack.charAt(messInit) == ' ') messInit++;
        event.setProgram(pack.substring(dataBegin, appEnd));
        event.setMessage(pack.substring(messInit));
    }
    
    private void initDataFailed(final ParsedEvent event,
                                final String pack, final int init) {
        final String data = (init == 0) ? pack : pack.substring(init);
        event.setData(data);
        event.setMessage(data);
    }
    
    private static int parseInt(final String data,
                                final int init, final int end) {
        int result = 0;
        for (int i = init; i < end; i++) {
            final char c = data.charAt(i);
            if (!Character.isDigit(c)) return -1;
            result = 10*result + (c - '0');
        }
        return result;
    }
}
