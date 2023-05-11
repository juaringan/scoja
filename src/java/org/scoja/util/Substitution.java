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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class complements pattern matching with the traditional expression
 * to extract matched groups and to build an string.
 */
public class Substitution {

    private static final Pattern holePattern
        = Pattern.compile("\\\\(([0-9])|(\\{([0-9]+)\\})|(.))");

    protected final String pattern;
    protected final String[] literals;
    protected final int[] groups;

    public Substitution(final String pattern) {
        final List ltrs = new ArrayList();
        final List grps = new ArrayList();
        final Matcher m = holePattern.matcher(pattern);
        final StringBuffer lit = new StringBuffer();
        String remain;
        int prev = 0;
        while (m.find()) {
            if (m.start(2) < m.end(2) || m.start(4) < m.end(4)) {
                remain = pattern.substring(prev, m.start(0));
                if (lit.length() == 0) {
                    ltrs.add(remain);
                } else {
                    lit.append(remain);
                    ltrs.add(lit.toString());
                    lit.delete(0, lit.length());
                }
                grps.add(
                    new Integer(m.group((m.start(2) < m.end(2)) ? 2 : 4)));
            } else {
                lit.append(pattern.substring(prev, m.start(0)));
                lit.append(m.group(5));
            }
            prev = m.end(0);
        }
        remain = pattern.substring(prev);
        if (lit.length() == 0) {
            ltrs.add(remain);
        } else {
            lit.append(remain);
            ltrs.add(lit.toString());
        }
        
        this.pattern = pattern;
        this.literals = (String[])ltrs.toArray(new String[0]);
        this.groups = new int[grps.size()];
        for (int i = 0; i < this.groups.length; i++) {
            this.groups[i] = ((Integer)grps.get(i)).intValue();
        }
    }
    
    public Substitution(final String[] literals, final int[] groups) {
        if (literals.length != groups.length+1) {
            throw new IllegalArgumentException(
                "Literals length should exceed groups length by 1");
        }
        this.literals = literals;
        this.groups = groups;
        this.pattern = null;
    }
    
    public String apply(final Matcher m) {
        final StringBuffer sb = new StringBuffer();
        applyTo(sb, m);
        return sb.toString();
    }
    
    public void applyTo(final StringBuffer sb, final Matcher m) {
        for (int i = 0; i < groups.length; i++) {
            sb.append(literals[i]);
            final String grp = m.group(groups[i]);
            if (grp != null) sb.append(grp);
        }
        sb.append(literals[literals.length-1]);
    }
    
    
    //======================================================================
    public String toString() {
        if (pattern != null) return pattern;
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < groups.length; i++) {
            sb.append(literals[i].replaceAll("\\\\", "\\\\\\\\"))
                .append("\\{").append(groups[i]).append('}');
        }
        sb.append(literals[literals.length-1].replaceAll("\\\\", "\\\\\\\\"));
        return sb.toString();
    }
    
    public int hashCode() {
        int code = 0;
        for (int i = 0; i < groups.length; i++) {
            code = (code << 3) + groups[i] + literals[i].hashCode();
        }
        code = (code << 3) + literals[literals.length-1].hashCode();
        return code;
    }
    
    public boolean equals(final Object other) {
        return (other instanceof Substitution)
            && equals((Substitution)other);
    }
    
    public boolean equals(final Substitution other) {
        if (other == null
            || this.literals.length != other.literals.length) return false;
        for (int i = 0; i < this.groups.length; i++) {
            if (this.groups[i] != other.groups[i]) return false;
        }
        for (int i = 0; i < this.literals.length; i++) {
            if (!this.literals[i].equals(other.literals[i])) return false;
        }
        return true;
    }
}
