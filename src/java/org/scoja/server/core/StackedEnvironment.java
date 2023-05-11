
package org.scoja.server.core;

import java.util.HashMap;

/**
 * A simple implementation of {@link Environment} with no special
 * emphasis in any operation.
 */
public class StackedEnvironment implements Environment {

    private static final int INITIAL_FRAME_SIZE = 3;

    protected HashMap[] stack;
    protected int top;

    public StackedEnvironment() {
        this.stack = null;
        this.top = 0;
    }
    
    public void mark() {
        top++;
    }
    
    public void release() {
        if (top > 0) {
            if (top < stack.length) stack[top] = null;
            top--;
        }
    }

    public boolean isDefined(final String var) {
        if (stack == null) return false;
        for (int i = Math.min(top, stack.length); i >= 0; i--) {
            final HashMap frame = stack[i];
            if (frame != null && frame.containsKey(var)) return true;
        }
        return false;
    }

    public QStr definition(final String var) {
        if (stack == null) return null;
        for (int i = Math.min(top, stack.length); i >= 0; i--) {
            final HashMap frame = stack[i];
            if (frame != null) {
                final Object value = frame.get(var);
                if (value != null) return (QStr)value;
            }
        }
        return null;
    }
    
    public void define(final String var, final String value) {
        define(var, new QStr(value));
    }
    
    public void define(final String var, final QStr value) {
        if (stack == null) {
            stack = new HashMap[2*top+1];
        } else if (stack.length <= top) {
            final HashMap[] newStack = new HashMap[2*top+1];
            for (int i = 0; i < stack.length; i++) newStack[i] = stack[i];
            stack = newStack;
        }
        if (stack[top] == null) {
            stack[top] = new HashMap(INITIAL_FRAME_SIZE);
        }
        stack[top].put(var, value);
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
        final StringBuffer sb = new StringBuffer("StackedEnvironment[");
        for (int i = 0; i <= top; i++) {
            if (i > 0) sb.append("; ");
            final HashMap frame
                = (stack != null && i < stack.length) ? stack[i] : null;
            sb.append(i).append(":").append(frame);
        }
        sb.append(']');
        return sb.toString();
    }
}
