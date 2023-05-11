
package org.scoja.server.core;

import java.util.HashMap;

/**
 * This implementation of {@link Environment} tries to make
 * {@link #mark()} and {@link #release()} as cheap as possible.
 * Consecutive {@link #mark()} executions, with no intervening calls
 * to {@link #define(String,String)}, involve no object construction.
 */
public class LazyStackedEnvironment implements Environment {

    protected final HashMap map;
    protected int marks;
    protected Environment local;
    protected int localMarks;
    
    public LazyStackedEnvironment() {
        this.map = new HashMap(3);
        this.marks = 0;
        this.local = null;
        this.localMarks = 0;
    }
    
    public void mark() {
        if (map.isEmpty()) {
            marks++;
        } else {
            localMarks++;
            if (local == null) local = new LazyStackedEnvironment();
            local.mark();
        }
    }
    
    public void release() {
        if (localMarks == 0) {
            map.clear();
            marks--;
        } else {
            localMarks--;
            local.release();
        }
    }

    public void define(final String var, final String value) {
        define(var, new QStr(value));
    }
    
    public void define(final String var, final QStr value) {
        if (localMarks == 0) {
            map.put(var, value);
        } else {
            local.define(var, value);
        }
    }
    
    public boolean isDefined(final String var) {
        if (localMarks > 0) {
            final QStr value = local.definition(var);
            if (value != null) return true;
        }
        return map.containsKey(var);
    }
    
    public QStr definition(final String var) {
        if (localMarks > 0) {
            final QStr value = local.definition(var);
            if (value != null) return value;
        }
        return (QStr)map.get(var);
    }
    
    public void unknown(final String value) {
        unknown(new QStr(value));
    }
    
    public void unknown(final QStr value) {
        define(null, value);
    }
    
    public QStr unknown() {
        final QStr value = definition(null);
        return (value == null) ? Q_UNKNOWN : value;
    }
    
    //======================================================================
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("Environment[Map: ").append(map)
            .append(", marks: ").append(marks)
            .append(", localMarks: ").append(localMarks);
        if (localMarks > 0) {
            sb.append(", local: ").append(local);
        }
        sb.append(']');
        return sb.toString();
    }
}
