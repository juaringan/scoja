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

import java.util.*;
import java.text.ParseException;


/**
 * This a utility class to work with Syslog priorities.
 * Contains named constants for facilities and levels.
 * It has serveral methods to convert between priority integers and strings.
 */
public abstract class PriorityUtils {

    //======================================================================
    // LEVELS:

    /** To use when we a level is needed but we have no sensible
     * level value. */
    public static final int UNKNOWN_LEVEL = -1;
        
    /** Level: System is unusable. */
    public static final int EMERG   = 0;
    /** Level: Action must be taken immediately */
    public static final int ALERT   = 1;
    /** Level: Critical conditions */
    public static final int CRIT    = 2;
    /** Level: Error conditions */
    public static final int ERR     = 3;
    /** Level: Warning conditions */
    public static final int WARNING = 4;
    /** Level: Normal but significant condition */
    public static final int NOTICE  = 5;
    /** Level: Informational */
    public static final int INFO    = 6;
    /** Level: Debug-level messages */
    public static final int DEBUG   = 7;
    
    /** Current number of levels. */
    public static final int TOTAL_LEVELS = 8;


    //======================================================================
    // FACILITIES:
    
    /** To use when we a facility is needed but we have no sensible
     * facility value. */
    public static final int UNKNOWN_FACILITY = -1;

    /**
     * A pretty high value to choose no facility.
     * This is not such big that <tt>buildPriority(NO_FACILITY, DEBUG)</tt>
     * is equal to {@link #UNKNOWN_FACILITY}.
     */    
    public static final int NO_FACILITY = 0x0FFFFFFF;
        
    /** Facility: Kernel messages */
    public static final int KERN     = 0;
    /** Facility: Random user-level messages */
    public static final int USER     = 1;
    /** Facility: Mail system */
    public static final int MAIL     = 2;
    /** Facility: System daemons */
    public static final int DAEMON   = 3;
    /** Facility: Security/authorization messages */
    public static final int AUTH     = 4;
    /** Facility: Messages generated internally by syslogd */
    public static final int SYSLOG   = 5;
    /** Facility: Line printer subsystem */
    public static final int LPR      = 6;
    /** Facility: Network news subsystem */
    public static final int NEWS     = 7;
    /** Facility: UUCP subsystem */
    public static final int UUCP     = 8;
    /** Facility: Clock daemon */
    public static final int CRON     = 9;
    /** Facility: Security/authorization messages (private) (Linux) */
    public static final int AUTHPRIV = 10;
    /** Facility: Ftp daemon (Linux) */
    public static final int FTP      = 11;

    /* Codes from 12 upto 15 are reserved for system use */

    /** Facility: Reserved for local use */
    public static final int LOCAL0   = 16;
    /** Facility: Reserved for local use */
    public static final int LOCAL1   = 17;
    /** Facility: Reserved for local use */
    public static final int LOCAL2   = 18;
    /** Facility: Reserved for local use */
    public static final int LOCAL3   = 19;
    /** Facility: Reserved for local use */
    public static final int LOCAL4   = 20;
    /** Facility: Reserved for local use */
    public static final int LOCAL5   = 21;
    /** Facility: Reserved for local use */
    public static final int LOCAL6   = 22;
    /** Facility: Reserved for local use */
    public static final int LOCAL7   = 23;
 
    /* Current number of facilities */
    public static final int TOTAL_FACILITIES = 24;


    //======================================================================
    // PRIORITIES:
    
    /** To use when we a priority is needed but we have no sensible
     * priority value. */
    public static final int UNKNOWN_PRIORITY = -1;
    
    public static final int DEFAULT_PRIORITY = buildPriority(USER,NOTICE);

    /** Mask to extract level part from a priority. */
    public static final int LEVEL_MASK = 0x07;
    
    /** Mask to extract facility part from a priority (previous to
     * shift with {@link #FACILITY_SHIFT}).  */
    public static final int FACILITY_MASK = 0x7FFFFFFF;
    
    /** Shift to apply to a priority to get its facility. */
    public static final int FACILITY_SHIFT = 3;
    
    
    //======================================================================
    // NAMES:
 
    /**
     * All level names, sorted according theirs level number.
     * Some levels have more than one name; this is why we have a matrix
     * instead of a vector.
     * The first name is the modern one; the rest are deprecated names.
     */
    private static String[][] levelNames = {
        {"emerg", "panic"},  // 0
        {"alert"},           // 1
        {"crit"},            // 2
        {"err", "error"},    // 3
        {"warning", "warn"}, // 4
        {"notice"},          // 5
        {"info"},            // 6
        {"debug"},           // 7
    };
    
    /**
     * All facilities names, sorted according theirs facility number.
     * Facilities between {@link #FTP} and {@link #LOCAL0} have no name;
     * so this array contains <code>null</code> at these positions.
     */
    private static String[] facilityNames = {
        "kern",        // 0
        "user",        // 1
        "mail",        // 2
        "daemon",      // 3
        "auth",        // 4
        "syslog",      // 5
        "lpr",         // 6
        "news",        // 7
        "uucp",        // 8
        "cron",        // 9
        "authpriv",    // 10
        "ftp",         // 11
        null,          // 12
        null,          // 13
        null,          // 14
        null,          // 15
        "local0",      // 16
        "local1",      // 17
        "local2",      // 18
        "local3",      // 19
        "local4",      // 20
        "local5",      // 21
        "local6",      // 22
        "local7",      // 23
        "security"     // Deprecated
    };
    
    
    /** 
     * A table to convert from level name to level value.
     * It is a mapping from String to Integer.
     * It is filled with values from {@link #levelNames}.
     */
    private static Map name2level;
    static {
        name2level = new HashMap(levelNames.length);
        for (int i = 0; i < levelNames.length; i++) {
            for (int j = 0; j < levelNames[i].length; j++) {
                name2level.put(levelNames[i][j], new Integer(i));
            }
        }
    }
    
    /**
     * A table to convert from facility name to facility value;
     * It is a mapping from String to Integer.
     * It is filled with non null entries of {@link #facilityNames}.
     */
    private static Map name2facility;
    static {
        name2facility = new HashMap(facilityNames.length);
        for (int i = 0; i < facilityNames.length; i++) {
            if (facilityNames[i] != null) {
                name2facility.put(facilityNames[i], new Integer(i));
            }
        }
    }
    

    //======================================================================
    // METHODS:
    
    /**
     * Return the facility part of a priority.
     */
    public static int getFacility(final int priority) {
        return (priority & FACILITY_MASK) >> FACILITY_SHIFT;
    }
    
    public static int getNonLevel(final int priority) {
        return priority >> FACILITY_SHIFT;
    }

    /**
     * Return the level part of a priority.
     */
    public static int getLevel(final int priority) {
        return priority & LEVEL_MASK;
    }

    /**
     * Builds a priority from a facility and a level.
     */
    public static int buildPriority(final int facility, final int level) {
        return (facility << 3) | level;
    }
    
    public static int setFacility(final int priority, final int facility) {
        return buildPriority(facility, getLevel(priority));
    }

    /**
     * Return the name of <code>facility</code> if it is known;
     * <code>null</code> otherwise.
     */
    public static String getFacilityName(final int facility) {
        return getFacilityName(facility, null);
    }
    
    public static String getFacilityName(final int facility,
            final String unknown) {
        return (0 <= facility && facility < TOTAL_FACILITIES) 
            ? facilityNames[facility] : unknown;
    }
    
    /**
     * Return the name of <code>level</code> if it is known;
     * <code>null</code> otherwise.
     */
    public static String getLevelName(final int level) {
        return getLevelName(level, null);
    }
    
    public static String getLevelName(final int level, final String unknown) {
        return (0 <= level && level < TOTAL_LEVELS)
            ? levelNames[level][0] : unknown;
    }
    
    /**
     * Return the name of <code>priority</code> if both its facility
     * and level are known; <code>null</code> otherwise.
     */
    public static String getPriorityName(final int priority) {
        final String facilityName = getFacilityName(getFacility(priority));
        if (facilityName == null) return null;
        final String levelName = getLevelName(getLevel(priority));
        if (levelName == null) return null;
        return facilityName + "." + levelName;
    }

    /**
     * Compares <code>facility</code> against known facility names
     * and returns it corresponding facility number.
     * Capitalization is irrelevant.
     * If <code>facility</code> is not a know facility name, returns -1.
     */
    public static int parseFacility(final String facility) {
        final String key = facility.toLowerCase();
        final Integer value = (Integer)name2facility.get(key);
        return (value != null) ? value.intValue() : UNKNOWN_FACILITY;
    }
    
    /**
     * Compares <code>level</code> against known level names
     * and returns it corresponding level number.
     * Capitalization is irrelevant.
     * @throws ParseException if <code>facility</code> is not a know
     * level name.
     */
    public static int parseLevel(final String level) {
        final String key = level.toLowerCase();
        final Integer value = (Integer)name2level.get(key);
        return (value != null) ? value.intValue() : UNKNOWN_LEVEL;
    }
    
    /**
     * Tries to build a priority number from its string description at
     * <code>priority</code>.
     */
    public static int parsePriority(final String priority) {
        try {
            return Integer.parseInt(priority);
        } catch(NumberFormatException e) {}
        
        final int dotIdx = priority.indexOf('.');
        if (dotIdx == -1) {
            final int facility = parseFacility(priority);
            if (facility != UNKNOWN_FACILITY) {
                return buildPriority(facility, NOTICE);
            }
            final int level = parseLevel(priority);
            if (level != UNKNOWN_LEVEL) {
                return buildPriority(USER, level);
            }
        } else {
            final int facility = parseFacility(priority.substring(0, dotIdx));
            final int level = parseLevel(priority.substring(dotIdx+1));
            if (facility != UNKNOWN_FACILITY && level != UNKNOWN_LEVEL) {
                return buildPriority(facility,level);
            }
        }
        return UNKNOWN_PRIORITY;
    }
 
}
