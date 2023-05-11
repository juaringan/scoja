
package org.scoja.server.core;

import java.io.PrintWriter;
import java.util.Calendar;
import java.net.InetAddress;

import org.scoja.common.PriorityUtils;

public class SyslogEvent extends EventSkeleton {

    public static final int DEFAULT_ENERGY = 5;
    public static final int DEFAULT_PRIORITY = 0;

    protected final InetAddress address;
    protected final char[] pack;
    protected final int length;
    protected int priority;
    protected long sendTimestamp;
    protected CharStr date;
    protected CharStr host;
    protected CharStr data;
    protected CharStr program;
    protected CharStr message;
    
    protected Calendar sendCalendar;

    public SyslogEvent(final InetAddress address,
                       final byte[] pack) {
        this(address, pack, 0, pack.length);
    }
    
    public SyslogEvent(final InetAddress address,
                       final byte[] data, final int offset, final int length) {
        super();
        this.address = address;
        final int end = findEnd(data, offset, offset+length);
        final int init = findInit(data, offset, end);
        this.pack = new char[end-init];
        int t = 0;
        for (int s = 0; s < this.pack.length; s++) {
            final int c = data[init+s] & 0xFF;
            if (c != '\n' && c != '\r') this.pack[t++] = (char)c;
        }
        this.length = t;
        parse();
    }
    
    private static boolean isSpace(final int b) {
        return 0 <= b && b <= ' ';
    }
    
    private static int findEnd(final byte[] data,
                               final int init, final int end) {
        int newEnd = end;
        while (init < newEnd && isSpace(data[newEnd-1])) {
            newEnd--;
        }
        return newEnd;
    }
    
    private static int findInit(final byte[] data,
                                final int init, final int end) {
        int newInit = init;
        while (newInit < end && isSpace(data[newInit])) {
            newInit++;
        }
        return newInit;
    }
    
    private static int find(final char[] data, final int init, final int c) {
        int i = init;
        while (i < data.length) {
            if (data[i] == c) return i;
            i++;
        }
        return -1;
    }
    
    private static boolean isChar(final char b) {
        return '0' <= b && b <= '9';
    }
    
    private static int parseInt(final char[] data,
                                final int init, final int end) {
        int result = 0;
        for (int i = init; i < end; i++) {
            if (!isChar(data[i])) return -1;
            result = 10*result + (data[i] - '0');
        }
        return result;
    }
    
    private static int parseInt2(final char[] data, final int i) {
        if (!isChar(data[i]) || !isChar(data[i+1])) return -1;
        return 10*(data[i]-'0') + (data[i+1]-'0');
    }
    
    private static int parseSpaceInt(final char[] data, final int i) {
        int result = 0;
        if (isChar(data[i])) result = data[i]-'0';
        if (isChar(data[i+1])) result = 10*result + (data[i+1]-'0');
        return result;
    }
    
    private static int parseMonth(final char[] data, final int init) {
	int month = -1;
	switch(data[init]) {
	case 'A':
	    if (data[init+1] == 'p' && data[init+2] == 'r') month = 3;
	    else if (data[init+1] == 'u' && data[init+2] == 'g') month = 7;
	    break;
	case 'D':
	    if (data[init+1] == 'e' && data[init+2] == 'c') month = 11;
	    break;
	case 'F':
	    if (data[init+1] == 'e' && data[init+2] == 'b') month = 1;
	    break;
	case 'J':
	    if(data[init+1] == 'a' && data[init+2] == 'n') month = 0;
	    else if(data[init+1] == 'u') {
		if(data[init+2] == 'l') month = 6;
		else if(data[init+2] == 'n') month = 5;
	    }
	    break;
	case 'M':
	    if(data[init+1] == 'a') {
		if(data[init+2] == 'r') month = 2;
		else if(data[init+2] == 'y') month = 4;
	    }
	    break;
	case 'N':
	    if(data[init+1] == 'o' && data[init+2] == 'v') month = 10;
	    break;
	case 'O':
	    if(data[init+1] == 'c' && data[init+2] == 't') month = 9;
	    break;
	case 'S':
	    if(data[init+1] == 'e' && data[init+2] == 'p') month = 8;
	    break;
	}
	return month;
    }
    
    
    protected void parse() {
        priority = DEFAULT_PRIORITY;
        this.sendTimestamp = UNKNOWN_TIMESTAMP;
        this.sendCalendar = null;
        date = null;
        
        int i = 0;
        
        if (pack[i] != '<') {
            initDataFailed(0);
            return;
        }
        
        final int priorityEnd = find(pack, i, '>');
        if (priorityEnd == -1) {
            initDataFailed(0);
            return;
        }
        
        priority = parseInt(pack, i+1, priorityEnd);
        if (priority == -1) priority = DEFAULT_PRIORITY;
        i = priorityEnd+1;
        
        if (i >= pack.length) {
            initDataFailed(i);
            return;
        }
        if (pack[i] != ' ') {
            if ((pack.length - i) >= DATE_LENGTH) {
                sendCalendar = parseDate(i);
                if (sendCalendar != null) {
                    sendTimestamp = sendCalendar.getTimeInMillis();
                    date = new CharStr(pack, i, DATE_LENGTH);
                    i += DATE_LENGTH;
                }
            }
        }
        
        while (i < pack.length && pack[i] == ' ') i++;
        final int hostBegin = i;
        while (i < pack.length && pack[i] != ' ' && pack[i] != ':') i++;
        int hostEnd, dataBegin;
        if (i >= pack.length || pack[i] == ':') {
            hostEnd = dataBegin = hostBegin;
            host = null;
        } else {
            hostEnd = i;
            host = new CharStr(pack, hostBegin, hostEnd-hostBegin);
            i++;
            while (i < pack.length && pack[i] == ' ') i++;
            dataBegin = i;
        }
        
        data = new CharStr(pack, dataBegin);
        final int appEnd = find(pack, i, ':');
        if (appEnd == -1) {
            program = new CharStr(pack, 0, 0);
            message = data;
            return;
        }
        int messInit = appEnd+1;
        if (messInit < pack.length && pack[messInit] == ' ') messInit++;
        program = new CharStr(pack, dataBegin, appEnd-dataBegin);
        message = new CharStr(pack, messInit, pack.length - messInit);
    }
    
    protected static final String DATE_EXAMPLE
        = "Dec 12 18:28:59";
        /* 012345678901234 */
    protected static final int DATE_LENGTH = DATE_EXAMPLE.length();
    
    protected Calendar parseDate(final int init) {
        final int month = parseMonth(pack, init);
        if (month == -1 || pack[init+3] != ' ') return null;
        final int day = parseSpaceInt(pack, init+4);
        if (day == -1 || pack[init+6] != ' ') return null;
        final int hour = parseInt2(pack, init+7);
        if (hour == -1 || pack[init+9] != ':') return null;
        final int minute = parseInt2(pack, init+10);
        if (minute == -1 || pack[init+12] != ':') return null;
        final int second = parseInt2(pack, init+13);
        if (second == -1) return null;
        final Calendar date = Calendar.getInstance();
        date.set(Calendar.MONTH, month);
        date.set(Calendar.DAY_OF_MONTH, day);
        date.set(Calendar.HOUR_OF_DAY, hour);
        date.set(Calendar.MINUTE, minute);
        date.set(Calendar.SECOND, second);
        return date;
    }
        
    protected void initDataFailed(final int dataInit) {
        data = new CharStr(pack, dataInit);
        program = new CharStr(pack, 0, 0);
        message = data;
    }

    //======================================================================
    public int getEnergy() {
        return DEFAULT_ENERGY;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public int getFacility() {
        return PriorityUtils.getFacility(priority);
    }
    
    public int getLevel() {
        return PriorityUtils.getLevel(priority);
    }
    
    public long getSendTimestamp() {
        return sendTimestamp;
    }
    
    public Calendar getSendCalendar() {
        if (sendCalendar == null) {
            sendCalendar = Calendar.getInstance();
            sendCalendar.setTimeInMillis(sendTimestamp);
        }
        return sendCalendar;
    }
    
    public long getReceivedTimestamp() {
        return buildTimestamp;
    }
    
    public Calendar getReceivedCalendar() {
        if (buildCalendar == null) {
            buildCalendar = Calendar.getInstance();
            buildCalendar.setTimeInMillis(buildTimestamp);
        }
        return buildCalendar;
    }
    
    public CharSequence getHost() {
        if (host != null) return host;
        return getAddress().getHostAddress();
    }
    
    public InetAddress getAddress() {
        return address;
    }
    
    public CharSequence getData() {
        return data;
    }
    
    public CharSequence getProgram() {
        return program;
    }
    
    public CharSequence getMessage() {
        return message;
    }

    public void writeTo(final PrintWriter out) {
        synchronized (out) {
            if (date != null) date.writeTo(out);
            else out.print("Mmm dd hh:mm:ss");
            out.print(' ');
            if (host != null) host.writeTo(out);
            else out.print(getAddress().getHostAddress());
            out.print(' ');
            data.writeTo(out);
            out.write('\n');
        }
    }
    
    //======================================================================
    public String toString() {
        return "Priority: " + getPriority()
            + ", send: " + getSendCalendar().getTime()
            + ", received: " + getReceivedCalendar().getTime()
            + ", host: " + getHost()
            + ", data: \"" + getData() + "\""
            + ", program: \"" + getProgram() + "\""
            + ", message: \"" + getMessage() + "\"";
    }
}
