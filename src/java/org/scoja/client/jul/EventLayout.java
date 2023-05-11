/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogRecord;
import java.util.regex.*;

/**
 * An event layout is a micro language to define the text format of a
 * log record.
 * It is a secuence of literal text and holes.
 * Literal text produces just itself.
 * Holes are references to data in the log record.
 * Holes are marked with a <tt>$</tt>.
 * Just after the <tt>$</tt>, the name of the hole, possible with arguments,
 * follows.
 * Holes whose name is not a sequence of letters or number,
 * or with arguments should be surrounded with braces.
 * 
 * <p><b>Holes without arguments:</b>
 * <dl>
 * <dt>class
 * <dd>The class name that is the source of this log record.
 *
 * <dt>method
 * <dd>The method name that is the source of this log record.
 *
 * <dt>thread
 * <dd>The id of the thread that created this log record.
 *
 * <dt>sequence
 * <dd>This log record sequence number.
 *
 * <dt>level
 * <dd>The level name.
 *
 * <dt>level#
 * <dd>The level numerical value
 *
 * <dt>exceptionclass
 * <dd>If the log record has an exception, its class name.
 *     Otherwise, empty.
 * </dl>
 *
 * <p><b>Holes with parameters.</b>
 * All these holes have one or more parameters.
 * All the parameters are optionals, with sensible defaults.
 * The parameters are separated with a colon (<tt>:</tt>).
 * For example, the <tt>date</tt> hole allows a parameter with the date format;
 * if we want only the date without the time: <tt>${date:yy-MM-dd}</tt>.
 * Most of these holes allow a escaping parameter, that it is really two
 * parameters: one with the espacing method, and other with the list of escaped
 * characters (non visible characters are written with C escaping sequences).
 * For example, to change all the pipes <tt>|</tt> in the message for its
 * equivalente URL encoding: <tt>${message:URL:|}</tt>.
 * <dl>
 * <dt>logger
 * <dd>The logger name.
 *     Allows an escaping argument; defaults to no escaping.
 * 
 * <dt>date
 * <dd>The timestamp of the log record allocation.
 *     Allows an argument with the format of the timestamp that will be
 *     given to java.text.SimpleDateFormat.
 *     Defaults to <tt>yyyy-MM-dd HH:mm:ss.SSS</tt>.
 *
 * <dt>message
 * <dd>The message.
 *     Allows an escaping argument; default to no escaping.
 *
 * <dt>parameter
 * <dd>A parameter of the log record.
 *     Has two parameter.
 *     First, the index of the parameter; defaults to 0.
 *     Second, an escaping argument; defaults to no escaping.
 * <dt>exceptionmessage
 * <dd>If the log record has an exception, its message; otherwise, empty.
 *     Allows an escaping argument; defaults to no escaping.
 *     
 * <dt>stacktrace
 * <dd>If the log record has an exception, its stack trace; otherwise, empty.
 *     Allows an escaping argument; defaults to no escaping.
 * </dl>
 */
public abstract class EventLayout {

    //======================================================================
    public boolean isEmpty() {
        return false;
    }
    
    public EventLayout simplified() {
        return this;
    }
    
    public void format(final StringBuffer target, final LogRecord lr) {
        target.append(format(lr));
    }
    
    public String format(final LogRecord lr) {
        final StringBuffer sb = new StringBuffer();
        format(sb, lr);
        return sb.toString();
    }

    
    //======================================================================
    public static EventLayout parse(final String template) {
        final Matcher matcher = paramPattern.matcher(template);
        final List parts = new ArrayList();
        int prevEnd = 0;
        while (matcher.find()) {
            final int curStart = matcher.start(0);
            if (prevEnd < curStart) {
                final String verb = template.substring(prevEnd,curStart);
                parts.add(new Literal(verb));
            }
            if (matcher.group(3) != null) {
                parts.add(new Literal("$"));
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
            parts.add(new Literal(verb));
        }
        return new Composite(parts).simplified();
    }
    
    private static final String PARAM_EXPR
        = "(?:\\$([a-zA-Z0-9]+))|(?:\\$\\{([^{}]+)\\})|(?:\\$(\\$))";
        
    private static final Pattern paramPattern = Pattern.compile(PARAM_EXPR);

    private static EventLayout buildNamedHole(final String fullhole) {
        final String key, value;
        final int colon = fullhole.indexOf(':');
        if (colon == -1) {
            key = fullhole;
            value = null;
        } else {
            key = fullhole.substring(0,colon);
            value = fullhole.substring(colon+1);
        }
        final Class holeclazz = (Class)holes.get(key.toLowerCase());
        if (holeclazz == null) {
            throw new IllegalArgumentException("Unknown hole " + key);
        }
        final Hole hole;
        try {
            hole = (Hole)holeclazz.newInstance();
        } catch (Exception shouldNotHappen) {
            return new Literal("${" + fullhole + "}");
        }
        if (value != null) hole.with(value);
        return hole;
    }
    
    private static final Map holes;
    static {
        holes = new HashMap();
        holes.put("class", SourceClass.class);
        holes.put("date", Timestamp.class);
        holes.put("exceptionclass", ExceptionClass.class);
        holes.put("exceptionmessage", ExceptionMessage.class);
        holes.put("level", LevelName.class);
        holes.put("level#", LevelValue.class);
        holes.put("logger", LoggerName.class);
        holes.put("message", Message.class);
        holes.put("method", SourceMethod.class);
        holes.put("parameter", Parameter.class);
        holes.put("sequence", Sequence.class);
        holes.put("stacktrace", StackTrace.class);
        holes.put("thread", ThreadID.class);
    }    
}
