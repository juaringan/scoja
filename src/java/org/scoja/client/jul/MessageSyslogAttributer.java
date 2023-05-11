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

import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Handler;
import java.util.logging.StreamHandler;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This Handler behaves as an attribute enhancer for SyslogAttribute.
 * If the LogRecord has no SyslogAttribute,
 * or it has a SyslogAttribute not fully instantiated,
 * it looks for a pattern in the message;
 * if the pattern is found, and if the value matched is found in a table,
 * then the LogRecord is extended with an attribute (attributing).
 * Otherwise (the LogRecord has a fully instantited SyslogAttribute,
 * or the pattern didn't match, or the match is not found in the table),
 * the LogRecord is kept unmodified (keeping).
 * In any case, the LogRecord is passed away to the next Handler (target).
 *
 * <p><b>Configuration</b>
 * These are the recognized property names.
 * There are several properties that really are a family of properties, indexed
 * by a consecutive integer numbers starting with 1.
 * Default value are stablished defining the property without the index.
 * <dl>
 * <dt>org.scoja.client.jul.MessageSyslogAttributer.regex
 * <dd>The regular expression to look for in the message.
 *     If the expression is not found, this Handler has no effect on the
 *     LogRecord; it is passed to the <i>target</i> handler without
 *     modification.
 *     Optional; defaults to {@link #DEFAULT_PATTERN}
 *
 * <dt>org.scoja.client.jul.MessageSyslogAttributer.target
 * <dd>Class name for the next handler.
 *     Every call to {@link #publish} produces one call to the target.
 *     Compulsory.
 *
 * <dt>org.scoja.client.jul.MessageSyslogAttributer.keyGroup
 * <dd>If the regex is found, the value of this group is looked up in the
 *     attributes table.
 *     Optional; defaults to {@link #DEFAULT_GROUP}.
 *
 * <dt>org.scoja.client.jul.MessageSyslogAttributer.key-<i>n</i>
 * <dd>The keys for the attributes table.
 *     Index should start at 1 and should be consecutive.
 *
 * <dt>org.scoja.client.jul.MessageSyslogAttributer.priority-<i>n</i>
 * <dd>Priority for the case <i>n</i>.
 *     Must match <i>facility</i>.<i>level</i>.
 *     Either facility or level can be absent.
 *     If there is no dot, level is supposed.
 *
 * <dt>org.scoja.client.jul.MessageSyslogAttributer.tag-<i>n</i>,
 *     org.scoja.client.jul.MessageSyslogAttributer.program-<i>n</i>
 * <dd>Tag (aka program) for the case <i>n</i>.
 *
 * <dt>org.scoja.client.jul.MessageSyslogAttributer.message-<i>n</i>
 * <dd>Message for the case <i>n</i>.
 *
 * <dt>org.scoja.client.jul.MessageSyslogAttributer.dropGroup
 * <dd>When <i>attributing</i>, if this attribute is non-negative,
 *     this group capture is dropped from the message.
 *     Optional; defaults to {@link #DEFAULT_GROUP}.
 * </dl>
 *
 * <p>
 * It is important to realize that property files expand backslash sequences.
 * So, to escape a character in an regular expression, a double backslash is
 * needed.
 * Because escaped character in layout definitions use a C-like notation, 
 * some characters can be expanded at property-file level or at layout parsing.
 * That is, <tt>${exceptionmessage:URL:|\r\n}</tt> is
 * equivalent to <tt>${exceptionmessage:URL:|\\r\\n}</tt>.
 *
 * <p><b>Example</b>
 * This is a possible Discoplayer configuration.
 * <pre><tt>
 * handlers = org.scoja.client.jul.MessageSyslogAttributer    
 * org.scoja.client.jul.MessageSyslogAttributer.pattern = ^([^|]+)\\|
 * org.scoja.client.jul.MessageSyslogAttributer.keyGroup = 1
 * org.scoja.client.jul.MessageSyslogAttributer.dropGroup = 0
 * org.scoja.client.jul.MessageSyslogAttributer.priority = local5.
 * org.scoja.client.jul.MessageSyslogAttributer.message =                \
 *   ${message}|${exceptionmessage:URL:|\r\n}|${stacktrace:URL:\r\n}
 * org.scoja.client.jul.MessageSyslogAttributer.key-1 = exec
 * org.scoja.client.jul.MessageSyslogAttributer.tag-1 =  \
 *   SegInf.engine.discoplayer.localhost.exec
 * org.scoja.client.jul.MessageSyslogAttributer.key-2 = conf
 * org.scoja.client.jul.MessageSyslogAttributer.tag-2 =  \
 *   SegInf.engine.discoplayer.localhost.conf
 * org.scoja.client.jul.MessageSyslogAttributer.target = \
 *   org.scoja.client.jul.SyslogHandler
 * </tt></pre>
 *
 * @todo
 * <p><b>Improvements</b>
 * Compute key with a Template instead of with a matching group.
 * Compute tag with a Template instead of being fixed.
 * Both requires matching templates been added to Scoja/common.
 */
public class MessageSyslogAttributer extends Handler {

    private static final Logger log
        = Logger.getLogger(MessageSyslogAttributer.class.getName());

    
    protected final Config config;
    
    public MessageSyslogAttributer() {
        this(new Config().loadFromManager());
    }
    
    public MessageSyslogAttributer(final Config config) {
        this.config = config;
    }
    
    public Config getConfig() { return config; }
    
    public void publish(final LogRecord record) {
        //System.err.println("MessageSyslogAttributer#publish(" +record+ ")");
        AttributedLogRecord alr = null;
        SyslogAttribute sysattr = null;
        if (record instanceof AttributedLogRecord) {
            alr = (AttributedLogRecord)record;
            final Object attr = alr.getAttribute(
                SyslogAttribute.NAME, SyslogAttribute.class);
            if (attr instanceof SyslogAttribute) 
                sysattr = (SyslogAttribute)attr;
            if (sysattr != null && sysattr.isFullyInstantiated()) {
                config.getTarget().publish(record);
                return;
            }
        }
    
        SyslogAttribute newattr = null;
        final Matcher matcher = config.pattern.matcher(record.getMessage());
        if (matcher.find()) {
            newattr = config.key2attr.get(matcher.group(config.keyGroup));
        }
        if (newattr == null) {
            config.getTarget().publish(record);
            return;
        }
        
        //System.err.println("Found " + newattr);
        if (alr == null) alr = new AttributedLogRecord(record);
        if (config.dropGroup >= 0) {
            final int a = matcher.start(config.dropGroup);
            final int b = matcher.end(config.dropGroup);
            final String oldmsg = alr.getMessage();
            final String newmsg = (a == 0) ? oldmsg.substring(b)
                : (b == oldmsg.length()) ? oldmsg.substring(0,a)
                : ((oldmsg.substring(0,a) + oldmsg.substring(b)));
            
            alr.setMessage(newmsg);
        }
        if (sysattr == null) {
            sysattr = new SyslogAttribute(newattr);
            alr.putAttribute(sysattr);
        } else {
            sysattr.fix(newattr);
        }
        config.getTarget().publish(alr);
    }
    
    public void flush() {
        config.getTarget().flush();
    }
    
    public void close() {
        config.getTarget().close();
    }
    
    public String toString() {
        return getClass().getName() + "[config: " + config + "]";
    }
    
    
    //======================================================================
    public static class Config {
        protected final String BASE = MessageSyslogAttributer.class.getName();
        
        public final String DEFAULT_PATTERN = "^\\w+";
        public final int DEFAULT_GROUP = 0;
        
        protected Pattern pattern;
        protected int keyGroup;
        protected int dropGroup;
        protected final Map<String,SyslogAttribute> key2attr;
        protected final Object targetLock;
        protected Handler target;
    
        public Config() {
            this.pattern = Pattern.compile(DEFAULT_PATTERN);
            this.keyGroup = this.dropGroup = DEFAULT_GROUP;
            this.key2attr = new HashMap<String,SyslogAttribute>();
            this.targetLock = new Object();
            this.target = null;
        }
    
        public Config loadFromManager() {
            final String patstr = LogUtils.getString(BASE+".pattern");
            if (patstr != null) try {
                setPattern(patstr);
            } catch (Throwable e) {
                LogUtils.badValue(log, BASE+".pattern", patstr, e);
            }
            final String keystr = LogUtils.getString(BASE+".keyGroup");
            if (keystr != null) try {
                setKeyGroup(Integer.parseInt(keystr));
            } catch (Throwable e) {
                LogUtils.badValue(log, BASE+".keyGroup", keystr, e);
            }
            final String dropstr = LogUtils.getString(BASE+".dropGroup");
            if (dropstr != null) try {
                setDropGroup(Integer.parseInt(dropstr));
            } catch (Throwable e) {
                LogUtils.badValue(log, BASE+".dropGroup", dropstr, e);
            }
            final String trgstr = LogUtils.getString(BASE+".target");
            if (trgstr != null) try {
                setTarget(trgstr);
            } catch (Throwable e) {
                LogUtils.badValue(log, BASE+".target", trgstr, e);
            }
            final SyslogAttribute defAttr = new SyslogAttribute();
            loadAttribute(defAttr, "");
            for (int i = 1; ; i++) {
                final String key = LogUtils.getString(BASE+".key-"+i);
                if (key == null) break;
                final SyslogAttribute attr = new SyslogAttribute();
                loadAttribute(attr, "-"+i);
                attr.fix(defAttr);
                addCase(key, attr);
            }
            return this;
        }
        
        protected void loadAttribute(final SyslogAttribute attr,
                final String suffix) {
            final String pristr = LogUtils.getString(BASE+".priority"+suffix);
            if (pristr != null) try {
                attr.fixPriority(pristr);
            } catch (Throwable e) {
                LogUtils.badValue(log, BASE+".priority"+suffix, pristr, e);
            }
            String tag = LogUtils.getString(BASE+".program"+suffix);
            if (tag == null) tag = LogUtils.getString(BASE+".tag"+suffix);
            if (tag != null) try {
                attr.fixProgram(EventLayout.parse(tag));
            } catch (Throwable e) {
                LogUtils.badValue(log, BASE+".program"+suffix, pristr, e);
            }
            final String message = LogUtils.getString(BASE+".message"+suffix);
            if (message != null) try {
                attr.fixMessage(EventLayout.parse(message));
            } catch (Throwable e) {
                LogUtils.badValue(log, BASE+".message"+suffix, pristr, e);
            }
        }
        
        public void setPattern(final String pattern) {
            setPattern(Pattern.compile(pattern));
        }
    
        public void setPattern(final Pattern pattern) {
            if (pattern == null) throw new NullPointerException();
            this.pattern = pattern;
        }
    
        public void setKeyGroup(final int keyGroup) {
            this.keyGroup = Math.max(0, keyGroup);
        }
    
        public void setDropGroup(final int dropGroup) {
            this.dropGroup = Math.max(-1, dropGroup);
        }
    
        public void setTarget(final String trgClass)
        throws ClassNotFoundException, 
        InstantiationException, IllegalAccessException {
            setTarget(Class.forName(trgClass));
        }
    
        public void setTarget(final Class trgClass)
        throws InstantiationException, IllegalAccessException {
            setTarget((Handler)trgClass.newInstance());
        }
    
        public void setTarget(final Handler target) {
            this.target = target;
        }
    
        public void addCase(final String key, final SyslogAttribute value) {
            key2attr.put(key, value);
        }
        
        public void removeCase(final String key) {
            key2attr.remove(key);
        }
    
        protected Handler getTarget() {
            synchronized (targetLock) {
                if (target == null) target = new StreamHandler();
                return target;
            }
        }
        
        public String toString() {
            return getClass().getName()
                + "[pattern: " + pattern
                + ", key group: " + keyGroup
                + ", drop group: " + dropGroup
                + ", mapping: " + key2attr
                + ", target: " + target
                + "]";
        }
    }
}
