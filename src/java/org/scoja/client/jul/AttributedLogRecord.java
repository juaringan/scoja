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

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.scoja.cc.lang.Pair;

/**
 * Extends LogRecord with attributes.
 * Attributes are used to configure the behaviour of same handlers.
 * For instance, SyslogAttribute is used by SyslogHandler.
 */
public class AttributedLogRecord extends LogRecord {

    protected final Map<Pair<String,Class>,Object> attrs;
    
    public AttributedLogRecord(final Level level, final String msg) {
        super(level, msg);
        this.attrs = new HashMap<Pair<String,Class>,Object>(4);
    }
    
    public AttributedLogRecord(final LogRecord other) {
        super(other.getLevel(), other.getMessage());
        //setLevel();
        setLoggerName(other.getLoggerName());
        //setMessage();
        setMillis(other.getMillis());
        setParameters(other.getParameters());
        setResourceBundle(other.getResourceBundle());
        setResourceBundleName(other.getResourceBundleName());
        setSequenceNumber(other.getSequenceNumber());
        setSourceClassName(other.getSourceClassName());
        setSourceMethodName(other.getSourceMethodName());
        setThreadID(other.getThreadID());
        setThrown(other.getThrown());
        this.attrs = new HashMap<Pair<String,Class>,Object>(4);
    }
    
    public void putAttribute(final LogAttribute attr) {
        putAttribute(attr.getName(), attr.getClass(), attr);
    }
    
    public void putAttribute(final String name, final Object attr) {
        putAttribute(name, attr.getClass(), attr);
    }
    
    public void putAttribute(final String name, final Class clazz,
            final Object attr) {
        attrs.put(new Pair<String,Class>(name,clazz), attr);
    }
    
    public Object getAttribute(final String name, final Class clazz) {
        return attrs.get(new Pair<String,Class>(name, clazz));
    }
   
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AttributedLogRecord[")
            .append("time stamp: ").append(new Date(getMillis()))
            .append(", seq: ").append(getSequenceNumber())
            .append(", thread: ").append(getThreadID())
            .append(", location: ").append(getSourceClassName())
            .append('#').append(getSourceMethodName())
            .append(", level: ").append(getLevel())
            .append(", message: ").append(getMessage())
            .append(", thrown: ").append(getThrown())
            .append(", resource: ").append(getResourceBundleName())
            .append(", parameters: ");
        final Object[] params = getParameters();
        if (params != null) for (int i = 0; i < params.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(params[i]);
        }
        sb.append(", attributes: ").append(attrs)
            .append(']');
        return sb.toString();
    }
}
