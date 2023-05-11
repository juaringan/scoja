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

package org.scoja.server.core;

public class EventContext {

    protected final Event event;
    protected Environment environment;
    protected Link lastLink;
    protected boolean completed;

    public EventContext(final Event event, final Environment env) {
        this.event = event;
        this.environment = env;
        this.lastLink = null;
        this.completed = false;
    }
    
    public EventContext(final Event event) {
        this(event, null);
    }
    
    public Event getEvent() {
        return event;
    }
    
    public Environment getEnvironment() {
        if (environment == null) {
            environment = new StackedEnvironment();
        }
        return environment;
    }
    
    public Link getLastLink() {
        return lastLink;
    }
    
    public void setLastLink(final Link lastLink) {
        this.lastLink = lastLink;
    }
    
    public void process() {
        final Thread thread = Thread.currentThread();
        if (thread instanceof ScojaThread) {
            ((ScojaThread)thread).setEventContext(this);
        }
        getLastLink().propagate(this);
    }
    
    public boolean hasCompleted() {
        return completed;
    }
    
    public void complete() {
        completed = true;
    }
    
    
    //======================================================================
    public String toString() {
        return "EventContext"
            + "[event: " + event
            + ", environment: " + environment
            + ", completed: " + completed
            + ']';
    }
}
