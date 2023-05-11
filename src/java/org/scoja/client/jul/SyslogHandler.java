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

import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LoggingPermission;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.scoja.cc.lang.Structural;
import org.scoja.cc.text.escaping.URLLike;
import org.scoja.client.*;
import org.scoja.common.PriorityUtils;

/**
 * 
 */
public class SyslogHandler extends Handler {

    private static final Logger log
        = Logger.getLogger(SyslogHandler.class.getName());

    protected final Config config;
        
    public SyslogHandler() {
        this(new Config().loadFromManager());
    }
    
    public SyslogHandler(final Config config) {
        this.config = config;
    }
    
    public Config getConfig() { return config; }
    
    public void publish(final LogRecord record) {
        //System.err.println("SyslogHandler#publish(" + record + ")");
        final Date now = new Date(record.getMillis());
        int facility = config.facility;
        int level = PriorityUtils.UNKNOWN_LEVEL;
        EventLayout program = config.program;
        EventLayout message = config.message;
        if (record instanceof AttributedLogRecord) {
            final SyslogAttribute attr
                = (SyslogAttribute)((AttributedLogRecord)record).getAttribute(
                    SyslogAttribute.NAME, SyslogAttribute.class);
            if (attr != null) {
                facility = attr.getFacility(facility);
                level = attr.getLevel();
                program = attr.getProgram(program);
                message = attr.getMessage(message);
            }
        }
        if (level == PriorityUtils.UNKNOWN_LEVEL)
            level = config.syslogLevel(record.getLevel());
        final int priority = PriorityUtils.buildPriority(facility,level);
        final String prgstr = program.format(record);
        final String msgstr = message.format(record);
        try {
            config.getLogger().log(now, priority, null, prgstr, msgstr);
        } catch (Exception e) {
            error(msgstr, e, ErrorManager.WRITE_FAILURE);
        }
    }

    public void flush() {
        try {
            config.getLogger().flush();
        } catch (Exception e) {
            error("While flushing", e, ErrorManager.FLUSH_FAILURE);
        }
    }
    
    public void close()
    throws SecurityException {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new LoggingPermission("control",null));
        }
        try {
            config.getLogger().close();
        } catch (Exception e) {
            error("While closing", e, ErrorManager.CLOSE_FAILURE);
        }
    }

    protected void error(final String msg, final Exception e, final int code) {
        final ErrorManager onError = getErrorManager();
        if (onError != null) {
            onError.error(msg, e, code);
        }
    }
    
    public String toString() {
        return getClass().getName() + "[config: " + config + "]";
    }
    
    
    //======================================================================
    public static class Config {
    
        public static final Level DEBUG = Level.FINE;
        public static final Level INFO = Level.CONFIG;
        public static final Level NOTICE = Level.INFO;
        public static final Level WARNING = Level.WARNING;
        public static final Level ALERT = Level.SEVERE;
        public static final Level CRIT
            = Level.parse(
                Integer.toString((WARNING.intValue() + ALERT.intValue())/2));

        /**
         * <tt>DEBUG</tt>, <tt>INFO</tt>, <tt>NOTICE</tt>,
         * <tt>WARNING</tt>, <tt>CRIT</tt>, <tt>ALERT</tt>, <tt>EMERG</tt>, 
         */
        protected static final Level[] DEFAULT_J2S_LEVELS = { 
            DEBUG, INFO, NOTICE, WARNING, CRIT, ALERT,
        };

        private static final String PROP_BASE = SyslogHandler.class.getName();
    
        private static final String LEVEL_PROP = PROP_BASE + ".level";
        private static final String FILTER_PROP = PROP_BASE + ".filter";
    
        private static final String PROTOCOL_PROP = PROP_BASE + ".protocol";
        private static final String HOST_PROP = PROP_BASE + ".host";
        private static final String PORT_PROP = PROP_BASE + ".port";
        private static final String DEV_PROP = PROP_BASE + ".dev";
        private static final String PACKETLIMIT_PROP =PROP_BASE+".packetlimit";
        private static final String RETRIES_PROP = PROP_BASE + ".retries";
        private static final String FACILITY_PROP = PROP_BASE + ".facility";
        private static final String LEVELMAP_PROP = PROP_BASE + ".levelmap";
        private static final String PROGRAM_PROP = PROP_BASE + ".program";
        private static final String MESSAGE_PROP = PROP_BASE + ".message";
        private static final String TERMINATOR_PROP = PROP_BASE +".terminator";
    
        protected int defaultPacketLimit;
        protected int defaultRetries;
        protected String defaultTerminator;
        protected int facility;
        protected Level[] julLevels;
        protected EventLayout program;
        protected EventLayout message;
        protected final Object loggerLock;
        protected boolean defaultLogger;
        protected Syslogger logger;
        protected SpreadingSyslogger slogger;

        public Config() {
            //System.out.println("Building SyslogHandler");
            this.defaultPacketLimit = Syslogger.NO_PACKET_LIMIT;
            this.defaultRetries = 3;
            this.defaultTerminator = Syslogger.ZERO;
            this.facility = PriorityUtils.USER;
            this.julLevels = DEFAULT_J2S_LEVELS;
            this.program = new Literal("jul");
            this.message = new Message(URLLike.noControlSequence()); 
            this.loggerLock = new Object();
            this.defaultLogger = false;
            this.logger = null;
            this.slogger = null;
            //System.out.println("With logger: " + logger);
        }
    
        public Config loadFromManager() {
            setDefaultPacketLimit(
                LogUtils.getNat(PACKETLIMIT_PROP, defaultPacketLimit));
            setDefaultRetries(LogUtils.getNat(RETRIES_PROP, defaultRetries));
            setDefaultTerminatorByName(LogUtils.getString(TERMINATOR_PROP));
            try {
                setFacility(LogUtils.getString(FACILITY_PROP, "user"));
            } catch (final Exception e) {
                log.log(Level.WARNING, "Bad value for " + FACILITY_PROP, e);
            }
            try {
                setLevelMapping(LogUtils.getString(LEVELMAP_PROP));
            } catch (final Exception e) {
                log.log(Level.WARNING, "Bad value for " + LEVELMAP_PROP, e);
            }
            try {
                setProgram(LogUtils.getString(PROGRAM_PROP));
            } catch (final Exception e) {
                log.log(Level.WARNING, "Bad program template", e);
            }
            try {
                setMessage(LogUtils.getString(MESSAGE_PROP));
            } catch (final Exception e) {
                log.log(Level.WARNING, "Bad message template", e);
            }
        
            final String defaultProto
                = LogUtils.getString(PROTOCOL_PROP, "packet");
            String host = LogUtils.getString(HOST_PROP);
            if (host != null) {
                final int port = LogUtils.getPort(PORT_PROP, 514);
                try {
                    addNetLogger(defaultProto, host, port,
                            defaultPacketLimit, defaultRetries);
                } catch (Exception e) {
                    log.log(Level.WARNING, "Cannot build logger "
                            + defaultProto + ":" + host + ":" + port, e);
                }
            }
            String dev = LogUtils.getString(DEV_PROP);
            if (dev != null) {
                try {
                    addLocalLogger(defaultProto, dev,
                            defaultPacketLimit, defaultRetries);
                } catch (Exception e) {
                    log.log(Level.WARNING, "Cannot build logger "
                            + defaultProto + ":" + dev, e);
                }
            }
        
            int i = 0;
            for (;;) {
                i++;
                final String proto
                    = LogUtils.getString(PROTOCOL_PROP+"-"+i, defaultProto);
                final int packetLimit
                    = LogUtils.getNat(PACKETLIMIT_PROP+"-"+i, defaultPacketLimit);
                final int retries
                    = LogUtils.getNat(RETRIES_PROP+"-"+i, defaultRetries);
                host = LogUtils.getString(HOST_PROP+"-"+i);
                dev = LogUtils.getString(DEV_PROP + "-" + i);
                if (host == null && dev == null) break;
                if (host != null) {
                    final int port = LogUtils.getPort(PORT_PROP+"-"+i, 514);
                    try {
                        addNetLogger(proto, host, port, packetLimit, retries);
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Cannot build logger "
                                + proto + ":" + host + ":" + port, e);
                    }
                } else {
                    try {
                        addLocalLogger(proto, dev, packetLimit, retries);
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Cannot build logger "
                                + proto + ":" + dev, e);
                    }
                }
            }
            return this;
        }
    
        public void setDefaultPacketLimit(final int n) {
            this.defaultPacketLimit = n;
        }
    
        public void setDefaultRetries(final int n) {
            this.defaultRetries = Math.max(0, n);
        }
    
        public void setDefaultTerminator(final String terminator) {
            this.defaultTerminator = terminator;
        }
    
        public void setDefaultTerminatorByName(final String nAmE) {
            if (nAmE == null) return;
            final String name = nAmE.toLowerCase();
            if ("zero".equals(name)) setDefaultTerminator(Syslogger.ZERO);
            else if ("cr".equals(name)) setDefaultTerminator(Syslogger.CR);
            else if ("lf".equals(name)) setDefaultTerminator(Syslogger.LF);
            else if ("crlf".equals(name)) setDefaultTerminator(Syslogger.CRLF);
            else throw new IllegalArgumentException(
                "Unknown terminator " + nAmE);
        }
    
        public void setFacility(final String facility)
        throws ParseException {
            if (facility == null) return;
            this.facility = PriorityUtils.parseFacility(facility);
        }
    
        public void setLevelMapping(final String levels)
        throws IllegalArgumentException {
            if (levels == null) return;
            setLevelMapping(levels.split(","));
        }
    
        public void setLevelMapping(final String[] levelNames)
        throws IllegalArgumentException {
            if (levelNames.length != DEFAULT_J2S_LEVELS.length) {
                throw new IllegalArgumentException(
                    DEFAULT_J2S_LEVELS.length + " levels expected");
            }
            final Level[] levels = new Level[levelNames.length];
            for (int i = 0; i < levels.length; i++) {
                levels[i] = Level.parse(levelNames[i]);
            }
            this.julLevels = levels;
        }
    
        public void setProgram(final String template)
        throws IllegalArgumentException {
            if (template == null) return;
            setProgram(EventLayout.parse(template));
        }
    
        public void setProgram(final EventLayout program) {
            if (program == null) return;
            this.program = program;
        }
    
        public void setMessage(final String template)
        throws IllegalArgumentException {
            if (template == null) return;
            setMessage(EventLayout.parse(template));
        }
    
        public void setMessage(final EventLayout message) {
            if (message == null) return;
            this.message = message;
        }
    
        public void addNetLogger(final String proto,
                final String host, final int port,
                final int packetLimit, final int retries)
        throws SocketException, UnknownHostException, IllegalArgumentException{
            if (proto == null || "packet".equals(proto)) {
                addLogger(new UDPSyslogger(host,port)
                        .setPacketLimit(packetLimit), retries);
            } else if ("stream".equals(proto)) {
                addLogger(new ReusingTCPSyslogger(host,port)
                        .setPacketLimit(packetLimit), retries);
            } else {
                throw new IllegalArgumentException("Unknown protocol " +proto);
            }
        }
    
        public void addLocalLogger(final String proto, final String dev,
                final int packetLimit, final int retries)
        throws SocketException, UnknownHostException, IllegalArgumentException{
            if (proto == null || "packet".equals(proto)) {
                addLogger(new UnixDatagramSyslogger(dev)
                        .setPacketLimit(packetLimit), retries);
            } else if ("stream".equals(proto)) {
                addLogger(new ReusingUnixStreamSyslogger(dev)
                        .setPacketLimit(packetLimit), retries);
            } else {
                throw new IllegalArgumentException("Unknown protocol " +proto);
            }
        }
    
        public void addLogger(final Syslogger logger, final int retries) {
            addLogger(RetryingSyslogger.with(logger,retries,null));
        }
    
        public void addLogger(final Syslogger logger) {
            synchronized (loggerLock) {
                if (defaultLogger) {
                    try { 
                        this.logger.close();
                    } catch (Throwable e) {
                        log.log(Level.WARNING,
                                "While closing default logger", e);
                    }
                    this.logger = null;
                }
                if (this.logger == null) {
                    this.logger = logger;
                } else if (this.slogger == null) {
                    this.logger = this.slogger
                        = new SpreadingSyslogger(this.logger, logger);
                } else {
                    this.slogger.add(logger);
                }
            }
        }
    
        protected Syslogger getLogger()
        throws SocketException, UnknownHostException {
            synchronized (loggerLock) {
                if (logger == null) {
                    defaultLogger = true;
                    addLogger(new UDPSyslogger("localhost", 514)
                            .setPacketLimit(defaultPacketLimit),
                            defaultRetries);
                }
                return logger;
            }
        }
    
        protected int syslogLevel(final Level jl) {
            final int jlv = jl.intValue();
            return (jlv <= julLevels[3].intValue())
                ? ( (jlv <= julLevels[1].intValue())
                        ? ( (jlv <= julLevels[0].intValue())
                                ? PriorityUtils.DEBUG
                                : PriorityUtils.INFO )
                        : ( (jlv <= julLevels[2].intValue())
                                ? PriorityUtils.NOTICE
                                : PriorityUtils.WARNING ) 
                    )
                : ( (jlv <= julLevels[4].intValue())
                        ? PriorityUtils.CRIT
                        : ( (jlv <= julLevels[5].intValue())
                                ? PriorityUtils.ALERT
                                : PriorityUtils.EMERG )
                    );
        }
        
        public String toString() {
            return getClass().getName()
                + "[default packet limit: " + defaultPacketLimit
                + ", default retries: " + defaultRetries
                + ", default terminator: "
                + URLLike.noSpace().escaped(defaultTerminator)
                + ", facility: " + facility
                + ", levels: " + Structural.toString(julLevels)
                + ", program: " + program
                + ", message: " + message
                + ", default logger: " + defaultLogger
                + ", logger: " + logger
                + "]";
        }
    }
}
