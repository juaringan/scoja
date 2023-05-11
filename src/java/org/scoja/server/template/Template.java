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
package org.scoja.server.template;

import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.scoja.common.DateUtils;
import org.scoja.server.core.EventContext;
import org.scoja.server.expr.IP;
import org.scoja.server.expr.ResolvedIP;
import org.scoja.server.expr.CanonicalIP;
import org.scoja.server.expr.Host;
import org.scoja.server.expr.CanonicalHost;
import org.scoja.server.expr.Data;
import org.scoja.server.expr.Program;
import org.scoja.server.expr.Message;
import org.scoja.server.expr.PeerPrincipal;
import org.scoja.server.expr.PeerDN;

/**
 * Template are used to define file contents (events format)
 * and to build file names.
 * The second purpose has security concerns that are not present in the
 * first one.
 * So there are to families of methods to accomplish these two purposes:
 * {@link #toFilename()} and {@link #toFilename(EventContext)} generate
 * secure file names;
 * {@link #textFor(EventContext)}
 * and {@link #writeTo(PrintWriter,EventContext)}
 * generate file contents.
 */
public class Template
    extends EventWriter.Skeleton {

    private static final String PARAM_EXPR
        = "(?:\\$([a-zA-Z0-9]+))|(?:\\$\\{([^{}]+)\\})|(?:\\$(\\$))";
        
    private static final Pattern pattern = Pattern.compile(PARAM_EXPR);

    public static Template parse(final String template) {
        final Matcher matcher = pattern.matcher(template);
        final List parts = new ArrayList();
        int prevEnd = 0;
        while (matcher.find()) {
            final int curStart = matcher.start(0);
            if (prevEnd < curStart) {
                final String verb = template.substring(prevEnd,curStart);
                parts.add(new VerbatimPart(verb));
            }
            if (matcher.group(3) != null) {
                parts.add(new VerbatimPart("$"));
            } else {
                String var = matcher.group(1);
                if (var == null) var = matcher.group(2);
                parts.add(buildNamedHole(var));
            }
            prevEnd = matcher.end(0);
        }
        final int len = template.length();
        if (prevEnd < len) {
            final String verb = template.substring(prevEnd,len);
            parts.add(new VerbatimPart(verb));
        }
        return new Template(parts).simplified();
    }
    
    
    private static TemplatePart buildNamedHole(final String var) {
        TemplatePart part = (TemplatePart)var2part.get(var);
        if (part == null) part = new VarHole(var);
        return part;
    }
    
    
    private static final Map var2part = new HashMap();
    
    static {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() { init(); return null; }
        });
    }
     
    protected static void init() {
        InetAddress localhost = null;
        try {
            localhost = InetAddress.getLocalHost();
            putHole(new ConstantHole("MYIP", localhost.getHostAddress()));
            putHole(new ConstantHole("MYHOST", localhost.getHostName()));
            putHole(new ConstantHole("MYCHOST", 
                                     localhost.getCanonicalHostName()));
        } catch (UnknownHostException e) {
            System.err.println("Cannot find out localhost naming information;"
                               + " none of ${MYIP}, ${MYHOST} or ${MYCHOST}"
                               + " will be available");
        }
        final String me = System.getProperty("org.scoja.server.name");
        if (me != null || localhost != null) {
            final String value
                = (me != null) ? me : localhost.getHostName();
            putHole(new ConstantHole("ME",value));
        } else {
            System.err.println(
                "Property org.scoja.server.name is undefined"
                + " and cannot find out localhost naming information;"
                + " variable ${ME} will not be available");
        }
        
        putHole(new NamedFacilityHole("FACILITY"));
        putHole(new NamedLevelHole("LEVEL"));
        putHole(new NamedPriorityHole("PRIORITY"));
        putHole(new NumericalFacilityHole("FACILITY#"));
        putHole(new NumericalLevelHole("LEVEL#"));
        putHole(new NumericalPriorityHole("PRIORITY#"));
        
        //From the packet or connection IP
        putHole(new ValidFilenameStringHole("IP", new IP()));
        putHole(new ValidFilenameStringHole("RHOST", new ResolvedIP()));
        putHole(new ValidFilenameStringHole("CRHOST", new CanonicalIP()));
        //From the message host entry
        putHole(new ValidFilenameStringHole("HOST", new Host()));
        putHole(new ValidFilenameStringHole("CHOST", new CanonicalHost()));
        
        putHole(new InvalidFilenameStringHole("DATA", new Data()));
        final Hole program
            = new SuspiciousFilenameStringHole("PROGRAM", new Program());
        putHole(program);  putHole("PRG", program);
        final Hole message
            = new InvalidFilenameStringHole("MESSAGE", new Message());
        putHole(message);  putHole("MSG", message);
        
        final Hole peer
            = new SuspiciousFilenameStringHole("PEER", new PeerPrincipal());
        putHole(peer);  putHole("PEERDN", peer);
        putHole(new SuspiciousFilenameStringHole("PEERCN", new PeerDN("CN")));
        putHole(new SuspiciousFilenameStringHole("PEEROU", new PeerDN("OU")));
        putHole(new SuspiciousFilenameStringHole("PEERO", new PeerDN("O")));
        putHole(new SuspiciousFilenameStringHole("PEERL", new PeerDN("L")));
        putHole(new SuspiciousFilenameStringHole("PEERST", new PeerDN("ST")));
        putHole(new SuspiciousFilenameStringHole("PEERC", new PeerDN("C")));
        putHole(new SuspiciousFilenameStringHole("PEERSTREET",
                        new PeerDN("STREET")));
        putHole(new SuspiciousFilenameStringHole("PEERUID", new PeerDN("UID")));
        putHole(new SuspiciousFilenameStringHole("PEERDC", new PeerDN("DC")));
        
        putDateHoles(DateHoleSkeleton.SEND_TIMESTAMP);
        putDateHoles(DateHoleSkeleton.RECEPTION_TIMESTAMP);
        putDateHoles(DateHoleSkeleton.PREFERRED_TIMESTAMP);
    }
    
    private static void putDateHoles(final int whichTimestamp) {
        putHole(new StdTimestampHole("DATE", whichTimestamp));
        putHole(new StdTimestampHole("TIMESTAMP", whichTimestamp));
        putHole(new ShortTimestampHole("SHORTDATE", whichTimestamp));
        putHole(new ShortTimestampHole("SHORTTIMESTAMP", whichTimestamp));
        putHole(new LongTimestampHole("LONGDATE", whichTimestamp));
        putHole(new LongTimestampHole("LONGTIMESTAMP", whichTimestamp));
        putHole(new GMTTimestampHole("GMTDATE", whichTimestamp));
        putHole(new GMTTimestampHole("GMTTIMESTAMP", whichTimestamp));
        
        putHole(new NumberDateHole("YEAR", whichTimestamp, Calendar.YEAR));
        putHole(new ShortYearHole("_YEAR", whichTimestamp));
        
        putHole(new TwoDigitsDateHole("MONTH", whichTimestamp,
                                      Calendar.MONTH, 1));
        putHole(new NumberDateHole("_MONTH", whichTimestamp,
                                   Calendar.MONTH, 1));
        putHole(new StringDateHole("MONTHNAME", whichTimestamp,
                                   Calendar.MONTH, DateUtils.longMonthNames));
        putHole(new StringDateHole("_MONTHNAME", whichTimestamp,
                                   Calendar.MONTH, DateUtils.shortMonthNames));
        
        putHole(new TwoDigitsDateHole("DAY", whichTimestamp,
                                      Calendar.DAY_OF_MONTH));
        putHole(new NumberDateHole("_DAY", whichTimestamp,
                                   Calendar.DAY_OF_MONTH));
                                    
        putHole(new StringDateHole("WEEKDAY", whichTimestamp,
                                   Calendar.DAY_OF_WEEK, -1,
                                   DateUtils.longWeekDayNames));
        putHole(new StringDateHole("_WEEKDAY", whichTimestamp,
                                   Calendar.DAY_OF_WEEK, -1,
                                   DateUtils.shortWeekDayNames));
        
        putHole(new TwoDigitsDateHole("HOUR", whichTimestamp,
                                      Calendar.HOUR_OF_DAY));
        putHole(new NumberDateHole("_HOUR", whichTimestamp,
                                   Calendar.HOUR_OF_DAY));
        
        putHole(new TwoDigitsDateHole("MINUTE", whichTimestamp,
                                      Calendar.MINUTE));
        putHole(new NumberDateHole("_MINUTE",whichTimestamp, Calendar.MINUTE));
        putHole(new TwoDigitsDateHole("MIN", whichTimestamp, Calendar.MINUTE));
        putHole(new NumberDateHole("_MIN", whichTimestamp, Calendar.MINUTE));
        
        putHole(new TwoDigitsDateHole("SECOND", whichTimestamp,
                                      Calendar.SECOND));
        putHole(new NumberDateHole("_SECOND",whichTimestamp, Calendar.SECOND));
        putHole(new TwoDigitsDateHole("SEC", whichTimestamp, Calendar.SECOND));
        putHole(new NumberDateHole("_SEC", whichTimestamp, Calendar.SECOND));
        
        putHole(new ThreeDigitsDateHole("MILLISECOND", whichTimestamp,
                                        Calendar.MILLISECOND));
        putHole(new NumberDateHole("_MILLISECOND", whichTimestamp,
                                   Calendar.MILLISECOND));
        putHole(new ThreeDigitsDateHole("MILLIS", whichTimestamp,
                                        Calendar.MILLISECOND));
        putHole(new NumberDateHole("_MILLIS", whichTimestamp,
                                   Calendar.MILLISECOND));
        
        putHole(new EpochHole("EPOCH", whichTimestamp));
        putHole(new MilliepochHole("MILLIEPOCH", whichTimestamp));
        
        putHole(new TZHole("TZ", whichTimestamp));
        putHole(new TZOffsetHole("TZOFFSET", whichTimestamp));
        putHole(new GeneralTZOffsetHole("GTZOFFSET", whichTimestamp));
    }
    
    private static void putHole(final Hole hole) {
        putHole(hole.getVarName(), hole);
    }
    
    private static void putHole(final String key, final Hole hole) {
        var2part.put(key, hole);
    }

    
    //======================================================================
    protected final TemplatePart[] parts;

    public Template(final List parts) {
        this((TemplatePart[])parts.toArray(new TemplatePart[0]));
    }
    
    public Template(final TemplatePart[] parts) {
        this.parts = parts;
    }
    
    public Template simplified() {
        final List newParts = new ArrayList();
        TemplatePart prev = null;
        StringBuffer sb = null;
        int i = 0;
        while (i < parts.length || prev != null) {
            if (i < parts.length && parts[i].isConstant()) {
                if (prev == null) {
                    prev = parts[i];
                } else {
                    if (sb == null) {
                        sb = new StringBuffer();
                        prev.toFilename(sb);
                    }
                    parts[i].toFilename(sb);
                }
            } else {
                if (prev != null) {
                    if (sb == null) newParts.add(prev);
                    else newParts.add(new VerbatimPart(sb.toString()));
                    prev = null;  sb = null;
                }
                if (i < parts.length) newParts.add(parts[i]);
            }
            i++;
        }
        return new Template(newParts);
    }
    
    public EventWriter asEventWriter() {
        if (parts.length == 1) return parts[0];
        else return this;
    }
    
    public boolean isConstant() {
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isConstant()) return false;
        }
        return true;
    }
    
    public String toFilename() {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < parts.length; i++) {
            parts[i].toFilename(sb);
        }
        return sb.toString();
    }
    
    public String toFilename(final EventContext context) {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < parts.length; i++) {
            parts[i].toFilename(sb, context);
        }
        return sb.toString();
    }
    
    public void writeTo(final PrintWriter out, final EventContext context) {
        synchronized (out) {
            for (int i = 0; i < parts.length; i++) {
                parts[i].writeTo(out, context);
            }
        }
    }
    
    
    //======================================================================
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < parts.length; i++) {
            parts[i].toString(sb);
        }
        return sb.toString();
    }
    
    public String toString(final String sep) {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(sep);
            parts[i].toString(sb);
        }
        return sb.toString();
    }
    
    public boolean equals(final Object other) {
        return (other instanceof Template)
            && equals((Template)other);
    }
    
    public boolean equals(final Template other) {
        if (other == null
            || this.parts.length != other.parts.length) return false;
        for (int i = 0; i < this.parts.length; i++) {
            if (!this.parts[i].equals(other.parts[i])) return false;
        }
        return true;
    }
    
    public int hashCode() {
        int hash = 0;
        for (int i = 0; i < parts.length; i++) hash += parts[i].hashCode();
        return hash;
    }
}
