/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2010  Bankinter, S.A.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser/Library General Public License
 * as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
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
package org.scoja.client.jul;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.scoja.common.PriorityUtils;

public class SyslogAttribute implements LogAttribute {

    public static final String NAME = "syslog";

    protected int facility;
    protected int level;
    protected EventLayout program;
    protected EventLayout message;
    
    public SyslogAttribute() {
        this.facility = PriorityUtils.UNKNOWN_FACILITY;
        this.level = PriorityUtils.UNKNOWN_LEVEL;
        this.program = null;
        this.message = null;
    }
    
    public SyslogAttribute(final SyslogAttribute other) {
        this.facility = other.facility;
        this.level = other.level;
        this.program = other.program;
        this.message = other.message;
    }
    
    public String getName() { return NAME; }
    
    public boolean isFullyInstantiated() {
        return facility != PriorityUtils.UNKNOWN_FACILITY
            && level != PriorityUtils.UNKNOWN_LEVEL
            && program != null;
    }
    
    public int getFacility() { return facility; }
    public int getFacility(final int def) { 
        return (facility == PriorityUtils.UNKNOWN_FACILITY) ? def : facility; }
    public void setFacility(final int facility) { this.facility = facility; }
    public void fixFacility(final int facility) {
        if (this.facility == PriorityUtils.UNKNOWN_FACILITY)
            setFacility(facility);
    }
    
    public int getLevel() { return level; }
    public int getLevel(final int def) { 
        return (level == PriorityUtils.UNKNOWN_LEVEL) ? def : level; }
    public void setLevel(final int level) { this.level = level; }
    public void fixLevel(final int level) {
        if (this.level == PriorityUtils.UNKNOWN_LEVEL) setLevel(level);
    }
    
    public void fixPriority(final String pristr) {
        final int dot = pristr.indexOf('.');
        if (dot > 0) {
            fixFacility(PriorityUtils.parseFacility(pristr.substring(0,dot)));
        }
        if (dot < pristr.length()-1) {
            fixLevel(PriorityUtils.parseLevel(pristr.substring(dot+1)));
        }
    }
    
    public EventLayout getProgram() { return program; }
    public EventLayout getProgram(final EventLayout def) { 
        return (program == null) ? def : program; }
    public void setProgram(final EventLayout program) {this.program = program;}
    public void fixProgram(final EventLayout program) {
        if (this.program == null) setProgram(program);
    }
    
    public EventLayout getMessage() { return message; }
    public EventLayout getMessage(final EventLayout def) { 
        return (message == null) ? def : message; }
    public void setMessage(final EventLayout message) {this.message = message;}
    public void fixMessage(final EventLayout message) {
        if (this.message == null) setMessage(message);
    }
    
    public void fix(final SyslogAttribute other) {
        fixFacility(other.getFacility());
        fixLevel(other.getLevel());
        fixProgram(other.getProgram());
        fixMessage(other.getMessage());
    }
    
    public String toString() {
        return PriorityUtils.getFacilityName(facility,"?")
            + "." + PriorityUtils.getLevelName(level,"?")
            + "/" + (program == null ? "?" : program)
            + "/" + (message == null ? "?" : message);
    }
}
