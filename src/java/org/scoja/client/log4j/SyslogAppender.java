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

package org.scoja.client.log4j;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.URL;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import org.scoja.client.*;
import org.scoja.common.PriorityUtils;
import org.scoja.cc.text.escaping.URLLike;

/**
 * A Syslog Appender for log4j.
 * 
 * <p>Log4j properties file usage<br>
 * The only properties that are safe to configure when using a log4j properties
 * file are:
 * <dl>
 * <dt><tt>loggers</tt>
 * <dd>A comma separated sequence of syslog urls.
 *     The url must be correct.
 *     See {@link org.scoja.protocol.syslog.Handler} for syslog url
 *     documentation.
 *     The protocol part is not checked.
 *     Any protocol that manages all elements
 *     needed to configure a syslog connection is allowed
 *     (for instance, <i>http</i> protocol).
 *     Also, the protocol part is optional; when absent, this appender
 *     will try syslog:// and http://.
 *     Anyway, for documentation purposes, it is better to use syslog protocol.
 *     If the syslog protocol handler cannot be installed, it is better to
 *     specify no protocol and let this appender to do its best.
 *     This parameter is compulsory; if absent, this appender has no effect.
 * <dt><tt>facility</tt>
 * <dd>A syslog facility name or a number.
 *     (By default <tt>user</tt>.)
 * <dt><tt>program</tt>
 * <dd>Syslog tag name.
 *     It is processed with a PatternLayout, so <tt>%</tt> is an escape
 *     character to access to logging event properties.
 *     (By default <tt>log4j</tt>.)
 * <dt><tt>message</tt>
 * <dd>Event message.
 *     Like <tt>program</tt> is processed with a PatternLayout.
 *     (By default <tt>%c|%p|%m</tt>,
 *      that is, the source class, the level and the message.)
 * </dl>
 * This appender accepts layout;
 * it is an alternative and better way to define message format.
 */
public class SyslogAppender extends AppenderSkeleton {

    private static final Logger log
        = Logger.getLogger(SyslogAppender.class.getName());

    public static final Level DEBUG = Level.DEBUG;
    public static final Level INFO = Level.INFO;
    public static final Level NOTICE = Level.INFO;
    public static final Level WARNING = Level.WARN;
    public static final Level ALERT = Level.FATAL;
    public static final Level CRIT
        = Level.toLevel(
            Integer.toString((WARNING.toInt() + ALERT.toInt())/2));

    /**
     * <tt>DEBUG</tt>, <tt>INFO</tt>, <tt>NOTICE</tt>,
     * <tt>WARNING</tt>, <tt>CRIT</tt>, <tt>ALERT</tt>, <tt>EMERG</tt>, 
     */
    protected static final Level[] DEFAULT_J2S_LEVELS = { 
        DEBUG, INFO, NOTICE, WARNING, CRIT, ALERT,
    };

    private static final String PROP_BASE = SyslogAppender.class.getName();
    
    private static final String LEVEL_PROP = PROP_BASE + ".level";
    private static final String FILTER_PROP = PROP_BASE + ".filter";
    
    private static final String PROTOCOL_PROP = PROP_BASE + ".protocol";
    private static final String HOST_PROP = PROP_BASE + ".host";
    private static final String PORT_PROP = PROP_BASE + ".port";
    private static final String DEV_PROP = PROP_BASE + ".dev";
    private static final String PACKETLIMIT_PROP = PROP_BASE + ".packetlimit";
    private static final String RETRIES_PROP = PROP_BASE + ".retries";
    private static final String FACILITY_PROP = PROP_BASE + ".facility";
    private static final String LEVELMAP_PROP = PROP_BASE + ".levelmap";
    private static final String PROGRAM_PROP = PROP_BASE + ".program";
    private static final String MESSAGE_PROP = PROP_BASE + ".message";
    private static final String TERMINATOR_PROP = PROP_BASE + ".terminator";
    
    protected int defaultPacketLimit;
    protected int defaultRetries;
    protected String defaultTerminator;
    protected int facility;
    protected Level[] j2sLevels;
    protected Layout program;
    protected Syslogger logger;
    protected SpreadingSyslogger slogger;

    protected Properties properties;

    public SyslogAppender() {
        this.defaultPacketLimit = Syslogger.NO_PACKET_LIMIT;
        this.defaultRetries = 3;
        this.defaultTerminator = Syslogger.ZERO;
        this.facility = PriorityUtils.USER;
        this.j2sLevels = DEFAULT_J2S_LEVELS;
        this.program = new PatternLayout("log4j");
        this.layout = new PatternLayout("%c|%p|%m"); 
        this.logger = null;
        this.slogger = null;
    }
    
    public SyslogAppender(final Properties properties) {
        this();
        this.properties = properties;
        loadProperties();
    }
    
    
    //======================================================================
    // CONFIGURATION
    
    public SyslogAppender loadProperties() {
        setDefaultPacketLimit(getNat(PACKETLIMIT_PROP, defaultPacketLimit));
        setDefaultRetries(getNat(RETRIES_PROP, defaultRetries));
        setDefaultTerminatorByName(
            getString(TERMINATOR_PROP, defaultTerminator));
        try {
            setFacility(getString(FACILITY_PROP, "user"));
        } catch (final Exception e) {
            log.log(Level.WARN, "Bad value for " + FACILITY_PROP, e);
        }
        try {
            setLevelMapping(getString(LEVELMAP_PROP));
        } catch (final Exception e) {
            log.log(Level.WARN, "Bad value for " + LEVELMAP_PROP, e);
        }
        try {
            setProgram(getString(PROGRAM_PROP));
        } catch (final Exception e) {
            log.log(Level.WARN, "Bad program template", e);
        }
        try {
            setMessage(getString(MESSAGE_PROP));
        } catch (final Exception e) {
            System.err.println("Bad message template " + e);
            log.log(Level.WARN, "Bad message template", e);
        }
        
        final String defaultProto = getString(PROTOCOL_PROP, "packet");
        String host = getString(HOST_PROP);
        if (host != null) {
            final int port = getPort(PORT_PROP, 514);
            try {
                addNetLogger(defaultProto, host, port,
                             defaultPacketLimit, defaultRetries);
            } catch (Exception e) {
                log.log(Level.WARN, "Cannot build logger "
                        + defaultProto + ":" + host + ":" + port, e);
            }
        }
        String dev = getString(DEV_PROP);
        if (dev != null) {
            try {
                addLocalLogger(defaultProto, dev,
                               defaultPacketLimit, defaultRetries);
            } catch (Exception e) {
                log.log(Level.WARN, "Cannot build logger "
                        + defaultProto + ":" + dev, e);
            }
        }
        
        int i = 0;
        for (;;) {
            i++;
            final String proto = getString(PROTOCOL_PROP+"-"+i, defaultProto);
            final int packetLimit
                = getNat(PACKETLIMIT_PROP+"-"+i, defaultPacketLimit);
            final int retries = getNat(RETRIES_PROP+"-"+i, defaultRetries);
            host = getString(HOST_PROP+"-"+i);
            dev = getString(DEV_PROP + "-" + i);
            if (host == null && dev == null) break;
            if (host != null) {
                final int port = getPort(PORT_PROP+"-"+i, 514);
                try {
                    addNetLogger(proto, host, port, packetLimit, retries);
                } catch (Exception e) {
                    log.log(Level.WARN, "Cannot build logger "
                            + proto + ":" + host + ":" + port, e);
                }
            } else {
                try {
                    addLocalLogger(proto, dev, packetLimit, retries);
                } catch (Exception e) {
                    log.log(Level.WARN, "Cannot build logger "
                            + proto + ":" + dev, e);
                }
            }
        }
        return this;
    }
    
    public void setDefaultPacketLimit(final int n) {
        this.defaultPacketLimit = n;
    }

    public int getDefaultPacketLimit() {
        return this.defaultPacketLimit;
    }
    
    public void setDefaultRetries(final int n) {
        this.defaultRetries = Math.max(0, n);
    }

    public int getDefaultRetries() {
        return this.defaultRetries;
    }
    
    public void setDefaultTerminatorByName(final String nAmE) {
        final String name = nAmE.toLowerCase();
        if ("zero".equals(name)) defaultTerminator = Syslogger.ZERO;
        else if ("cr".equals(name)) defaultTerminator = Syslogger.CR;
        else if ("lf".equals(name)) defaultTerminator = Syslogger.LF;
        else if ("crlf".equals(name)) defaultTerminator = Syslogger.CRLF;
        else {
            log.log(Level.WARN, 
                    "Unknown terminator " + nAmE + "; will use current");
        }
    }
    
    public void setFacility(final String facility)
    throws ParseException {
        this.facility = PriorityUtils.parseFacility(facility);
    }
    
    public void setLevelMapping(final String levels)
    throws IllegalArgumentException {
        setLevelMapping(levels.split(","));
    }
    
    public void setLevelMapping(final String[] levelNames)
    throws IllegalArgumentException {
        if (levelNames.length != DEFAULT_J2S_LEVELS.length) {
            throw new IllegalArgumentException("Six levels expected");
        }
        final Level[] levels = new Level[levelNames.length];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = Level.toLevel(levelNames[i]);
        }
        this.j2sLevels = levels;
    }
    
    public void setProgram(final String template)
    throws IllegalArgumentException {
        setProgramLayout(new PatternLayout(template));
    }
    
    public void setProgramLayout(final Layout program) {
        this.program = program;
    }
    
    public void setMessage(final String template)
    throws IllegalArgumentException {
        setMessageLayout(new PatternLayout(template));
    }
    
    public void setMessageLayout(final Layout message) {
        setLayout(message);
    }
    
    public void addNetLogger(final String proto,
                             final String host, final int port,
                             final int packetLimit, final int retries)
    throws SocketException, UnknownHostException, IllegalArgumentException {
        if (proto == null || "packet".equals(proto)) {
            addLogger(new UDPSyslogger(host,port).setPacketLimit(packetLimit),
                      retries);
        } else if ("stream".equals(proto)) {
            addLogger(new ReusingTCPSyslogger(host,port)
                      .setPacketLimit(packetLimit), retries);
        } else {
            throw new IllegalArgumentException("Unknown protocol " + proto);
        }
    }
    
    public void addLocalLogger(final String proto, final String dev,
                               final int packetLimit, final int retries)
    throws SocketException, UnknownHostException, IllegalArgumentException {
        if (proto == null || "packet".equals(proto)) {
            addLogger(new UnixDatagramSyslogger(dev)
                      .setPacketLimit(packetLimit), retries);
        } else if ("stream".equals(proto)) {
            addLogger(new ReusingUnixStreamSyslogger(dev)
                      .setPacketLimit(packetLimit), retries);
        } else {
            throw new IllegalArgumentException("Unknown protocol " + proto);
        }
    }
    
    public void addLogger(final URL u)
    throws IOException {
        addLogger(new org.scoja.protocol.syslog.Handler().buildSyslogger(u));
    }
    
    public void setLoggers(final String loggerURLs)
    throws IOException {
        final Pattern hasProtocol = Pattern.compile("\\w+://");
        final StringTokenizer st = new StringTokenizer(loggerURLs, ",");
        while (st.hasMoreTokens()) {
            String urlstr = st.nextToken().trim();
            URL url = null;
            RuntimeException error = null;
            final String[] protocols
                = hasProtocol.matcher(urlstr).lookingAt() ? new String[] {""}
                : new String[] {"syslog://", "http://"};
            for (final String prefix: protocols) {
                try {
                    url = new URL(prefix + urlstr);
                    break;
                } catch (java.net.MalformedURLException e) {
                    if (error == null) {
                        error = new IllegalArgumentException(
                            e.getMessage()
                            + " (WARNING: if using `syslog' protocol,"
                            + " consider to add `org.scoja.protocol'"
                            + " to `java.protocol.handler.pkgs' standard"
                            + " property;"
                            + " if that doesn't work, consider to omit the"
                            + " protocol (the `syslog://' prefix)");
                        error.initCause(e);
                    }
                }
            }
            if (url != null) addLogger(url);
            else throw error;
        }
    }
    
    public void addLogger(final Syslogger logger, final int retries) {
        addLogger(RetryingSyslogger.with(logger,retries,null));
    }
    
    public void addLogger(final Syslogger logger) {
        if (this.logger == null) {
            this.logger = logger;
        } else if (this.slogger == null) {
            this.logger = this.slogger
                = new SpreadingSyslogger(this.logger, logger);
        } else {
            this.slogger.add(logger);
        }
    }
    
    public void append(final LoggingEvent event) {
        final int level = syslogLevel(event.getLevel());
        final int priority = PriorityUtils.buildPriority(facility,level);
        final String prgstr = program.format(event);;
        final String msgstr = layout.format(event);
        try {
            if (logger != null) {
                logger.log(priority, null, prgstr, msgstr);
                logger.flush();
            }
        } catch (Exception e) {
            try { logger.reset(); } catch (Exception ignored) {}
            error(msgstr, e, 1);
        }
    }

    public void close() {
        try {
            logger.close();
        } catch (LoggingException e) {
            error("While closing", e, 2);
        }
    }

    public boolean requiresLayout() {
        return true;
    }

    protected void error(final String msg, final Exception e, final int code) {
        errorHandler.error(msg, e, code);
    }
    
    protected int syslogLevel(final Level jl) {
        final int jlv = jl.toInt();
        return (jlv <= j2sLevels[3].toInt())
            ? ( (jlv <= j2sLevels[1].toInt())
                ? ( (jlv <= j2sLevels[0].toInt())
                    ? PriorityUtils.DEBUG
                    : PriorityUtils.INFO )
                : ( (jlv <= j2sLevels[2].toInt())
                    ? PriorityUtils.NOTICE
                    : PriorityUtils.WARNING ) 
                )
            : ( (jlv <= j2sLevels[4].toInt())
                ? PriorityUtils.CRIT
                : ( (jlv <= j2sLevels[5].toInt())
                    ? PriorityUtils.ALERT
                    : PriorityUtils.EMERG )
                );
    }
    
    
    //======================================================================
    protected String getString(final String key) {
        return this.properties.getProperty(key);
    }
    
    protected String getString(final String key, final String def) {
        final String value = getString(key);
        return (value == null) ? def : value;
    }
    
    protected int getNat(final String key, final int def) {
        final String strval = getString(key);
        if (strval == null) return def;
        final int value;
        try {
            value = Integer.parseInt(strval);
        } catch (final NumberFormatException e) {
            return def;
        }
        return (value < 0) ? def : value;
    }
    
    protected int getPort(final String key, final int def) {
        final String strval = getString(key);
        if (strval == null) return def;
        final int value;
        try {
            value = Integer.parseInt(strval);
        } catch (final NumberFormatException e) {
            return def;
        }
        return (value < 0 || value >= (1<<16)) ? def : value;
    }
}
