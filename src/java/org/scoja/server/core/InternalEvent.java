
package org.scoja.server.core;

import java.io.PrintWriter;
import java.util.Calendar;
import java.net.InetAddress;
import java.security.Principal;

import org.scoja.common.PriorityUtils;
import org.scoja.common.DateUtils;

/**
 * Los eventos internos se dividen en 3 grupos.
 * <ol>
 * <li><i>Errors</i>, with levels less or equal to {@link PriorityUtils#ERR}.
 * <li><i>Sensible events</i>, with levels 
 *     {@link PriorityUtils#WARNING} and {@link PriorityUtils#NOTICE}.
 * <li><i>Trace events</i>, with levels greater or equal to
 *     {@link PriorityUtils#INFO}.
 * </ol>
 * While processing <i>trace</i> events, no other internal events are
 * produced.
 *
 * <p>
 * A table to know when to use a level:
 * <dl>
 * <dt>{@link PriorityUtils#EMERG}
 * <dd>
 * <dt>{@link PriorityUtils#ALERT}
 * <dd>
 * <dt>{@link PriorityUtils#CRIT}
 * <dd>
 * <dt>{@link PriorityUtils#ERR}
 * <dd>
 * <dt>{@link PriorityUtils#WARNING}
 * <dd>
 * <dt>{@link PriorityUtils#NOTICE}
 * <dd>
 * <dt>{@link PriorityUtils#INFO}
 * <dd>
 * <dt>{@link PriorityUtils#DEBUG}
 * <dd>
 * </dl>
 */
public class InternalEvent extends EventSkeleton {
    
    protected final InetAddress address;
    protected final int energy;
    protected final int priority;
    protected String data;
    protected final String program;
    protected final String message;

    public InternalEvent(final InetAddress address,
                         final int energy, final int facility, final int level,
                         final String program, final String message) {
        this(address,
             energy, PriorityUtils.buildPriority(facility,level),
             program, message);
    }
    
    public InternalEvent(final InetAddress address,
                         final int energy, final int priority,
                         final String program, final String message) {
        super();
        this.address = address;
        this.energy = energy;
        this.priority = priority;
        this.data = null;
        this.program = program;
        this.message = message;
    }
    
    public InternalEvent(final long buildTimestamp,
                         final InetAddress address,
                         final int energy, final int priority,
                         final String program, final String message) {
        super(buildTimestamp);
        this.address = address;
        this.energy = energy;
        this.priority = priority;
        this.data = null;
        this.program = program;
        this.message = message;
    }
                         
    
    public int getEnergy() {
        return energy;
    }
    
    public int getByteSize() {
        return (3+1+2+1+2+1+2+1+2)
            + 1 + getHost().length()
            + 1 + getProgram().length()
            + 2 + getMessage().length();
    }
        
    public int getPriority() {
        return priority;
    }

    public boolean isTraceable() {
        final int level = getLevel();
        return level != PriorityUtils.DEBUG;
    }
    
    public boolean shouldLogErrors() {
        final int level = getLevel();
        return level != PriorityUtils.DEBUG && level != PriorityUtils.ERR;
    }
    
    public String getHost() {
        return getHostName();
    }
    
    public String getCanonicalHost() {
        return getCanonicalHostName();
    }
    
    public QStr getQCanonicalHost() {
        return getQCanonicalHostName();
    }
    
    public InetAddress getAddress() {
        return address;
    }
    
    public String getData() {
        if (data == null) {
            data = program + ": " + message;
        }
        return data;
    }
    
    public String getProgram() {
        return program;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Principal getPeerPrincipal() {
        return null;
    }
    
    public void writeTo(final PrintWriter out) {
        synchronized (out) {
            try {
                DateUtils.formatStd(out, getSendCalendar());
            } catch (java.io.IOException cannotHappen) {}
            out.print(' ');
            out.print(getHost());
            out.print(' ');
            out.print(getProgram());
            out.print(": ");
            out.print(getMessage());
            out.print('\n');
        }
    }
}
