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

package org.scoja.popu.recoja;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.scoja.util.Substitution;

/**
 * How to build a common filename from a piece filename.
 */
public class RewritingRule {

    protected final Pattern pattern;
    protected Action[] actions;
    protected Action[] optacts;

    public RewritingRule(final String regexp) 
    throws PatternSyntaxException {
        this.pattern = Pattern.compile(regexp);
        this.actions = new Action[10];
        this.optacts = null;
    }
    
    public RewritingRule drop(final int idx)
    throws IllegalStateException {
        return add(idx, new Drop(idx));
    }
    
    public RewritingRule substitute(final int idx, final Substitution subs)
    throws IllegalStateException {
        return add(idx, new Substitute(idx,subs));
    }
    
    protected RewritingRule add(final int idx, final Action action)
    throws IllegalStateException {
        optacts = null;
        ensureIndex(idx);
        if (actions[idx] != null) {
            throw new IllegalStateException(
                "Action for index " + idx + " already defined");
        }
        actions[idx] = action;
        return this;
    }
    
    public String rewrite(final String path) {
        final Matcher m = pattern.matcher(path);
        if (!m.find()) return null;
        optimize();
        final StringBuffer sb = new StringBuffer();
        int prev = 0;
        for (int i = 0; i < optacts.length; i++) {
            final int start = m.start(optacts[i].getIndex());
            if (prev > start) continue;
            sb.append(path.substring(prev,start));
            optacts[i].applyTo(sb, m);
            prev = m.end(optacts[i].getIndex());
        }
        sb.append(path.substring(prev));
        return sb.toString();
    }
    
    public void optimize() {
        if (optacts != null) return;
        int inUse = 0;
        for (int i = 0; i < actions.length; i++) {
            if (actions[i] != null) inUse++;
        }
        optacts = new Action[inUse];
        for (int i = 0, j = 0; i < actions.length; i++) {
            if (actions[i] != null) {
                optacts[j++] = actions[i];
            }
        }
    }
    
    protected void ensureIndex(final int idx) {
        if (idx < actions.length) return;
        final Action[] newActions
            = new Action[Math.max(2*actions.length,idx+1)];
        System.arraycopy(actions,0, newActions,0,actions.length);
        actions = newActions;
    }
    
    
    //----------------------------------------------------------------------
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("RewritingRule[")
            .append("\n  when: ").append(pattern.pattern());
        for (int i = 0; i < actions.length; i++) {
            if (actions[i] != null) {
                sb.append("\n  action: ").append(actions[i]);
            }
        }
        return sb.append("\n]").toString();
    }
    
    
    //======================================================================
    private static abstract class Action {
        protected final int idx;
        
        public Action(final int idx) {
            this.idx = idx;
        }
        
        public int getIndex() { return idx; }
        
        public abstract void applyTo(StringBuffer sb, Matcher m);
    }
    
    private static class Drop extends Action {
        public Drop(final int idx) {
            super(idx);
        }
        
        public void applyTo(final StringBuffer sb, final Matcher m) {}
        
        public String toString() {
            return "Drop[index: " + idx + "]";
        }
    }
    
    private static class Substitute extends Action {
        protected final Substitution subs;
        
        public Substitute(final int idx, final Substitution subs) {
            super(idx);
            this.subs = subs;
        }
        
        public void applyTo(final StringBuffer sb, final Matcher m) {
            subs.applyTo(sb, m);
        }
        
        public String toString() {
            return "Substitute[index: " + idx + ", with: " + subs + "]";
        }
    }
}
