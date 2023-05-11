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
package org.scoja.protocol.syslog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import org.scoja.cc.lang.Unit;
import org.scoja.common.PriorityUtils;
import org.scoja.util.ByteFall;
import org.scoja.util.MemoryByteFall;
import org.scoja.client.LoggingException;
import org.scoja.client.Syslogger;
import org.scoja.client.RetryingSyslogger;
import org.scoja.client.UDPSyslogger;
import org.scoja.client.UnixDatagramSyslogger;
import org.scoja.client.ReusingTCPSyslogger;
import org.scoja.client.ReusingTransportSyslogger;
import org.scoja.client.ReusingUnixStreamSyslogger;
import org.scoja.trans.Transport;
import org.scoja.trans.tcp.TCPConf;
import org.scoja.trans.tcp.TCPTransport;
import org.scoja.trans.nbtcp.NBTCPTransport;

/**
 * This handler extends URL with syslog protocol.
 * See {@link URL} to learn how to make this handler accessible to the JVM;
 * in a few words, property <tt>java.protocol.handler.pkgs</tt> should
 * contain <tt>org.scoja.protocol</tt>.
 *
 * <p><b>Protocol</b>
 * URL that makes reference to a server <t>sysser</tt>
 * receiving through UDP at port 666 is
 * <tt>syslog://sysser:666?method=packet</tt>
 * URL that makes reference to standard local Unix socket is
 * <tt>syslog:///dev/log?method=stream</tt>
 * With query parameters it is possible to configure all the syslog details.
 * There are parameters to configure the transport method:
 * <dl>
 * <dt><tt>method</tt>
 * <dd>One of <tt>packet</tt> or <tt>stream</tt>.
 *     Defaults to <tt>packet</tt>.
 * <dt><tt>block</tt>
 * <dd>It has effect only with stream method.
 *     A boolean to choose a blocking socket (<tt>true</tt>) or a non-blocking
 *     socket with a memory buffer (<tt>false</tt>).
 *     Defaults to <tt>true</tt>.
 * <dt><tt>maxbuffer</tt>
 * <dd>Used with non-blocking stream method.
 *     The max size of the buffer used to store events when the socket
 *     cannot transport more data without blocking.
 *     Detaults to <tt>1Mb</tt>.
 * <dt><tt>keepalive</tt>
 * <dd>Enables keep alive.
 *     It has effect only with stream method.
 *     Defaults to system default.
 * <dt><tt>buffersize</tt>
 * <dd>Send buffer size.
 *     Defaults to system default.
 * <dt><tt>linger</tt>
 * <dd>Linger.
 * <dt><tt>nodelay</tt>
 * <dd>No delay.
 * </dl>
 *
 * And parameters to configure the syslog event:
 * <dl>
 * <dt><tt>priority</tt>,
 * <dd>Default syslog priority.
 * <dt><tt>tag</tt>
 * <dd>Syslog tag or program name.
 * <dt><tt>hostname</tt>
 * <dd>Host name.
 * <dt><tt>senddate</tt>
 * <dd>Whether timestamp is send. Defaults to true.
 * <dt><tt>sendhost</tt>
 * <dd>Whether host is send. Defaults to true.
 * <dt><tt>terminator</tt>
 * <dd>Event terminator.
 * <dt><tt>packetlimit</tt>
 * <dd>Packet limit.
 * <dt><tt>retries</tt>
 * <dd>Retries on error.
 * </dl>
 */
public class Handler extends URLStreamHandler {

    public URLConnection openConnection(final URL u)
    throws IOException {
        final Syslogger logger = buildSyslogger(u);
        return new SysloggerURLConnection(u, logger);
    }
    
    public Syslogger buildSyslogger(final URL u)
    throws IOException {
        final Map<String,Object> prop = new HashMap<String,Object>();
        final String host = u.getHost();
        final String path = u.getPath();
        if (host != null && !"".equals(host)) {
            prop.put("host", host);
        } else if (path != null && !"".equals(path)) {
            prop.put("path", path);
        } else {
            prop.put("host", "localhost");
        }
        final int port
            = (u.getPort() != -1) ? u.getPort() : u.getDefaultPort();
        prop.put("port", new Integer(port));
        final String query = u.getQuery();
        if (query != null) {
            final StringTokenizer st = new StringTokenizer(u.getQuery(), "&");
            while (st.hasMoreTokens()) {
                final String kv = st.nextToken();
                final int eq = kv.indexOf('=');
                if (eq == -1) prop.put(kv, null);
                else prop.put(kv.substring(0,eq), kv.substring(eq+1));
            }
        }
        final Syslogger logger = buildSyslogger(prop);
        return logger;
    }
    
    public Syslogger buildSyslogger(final Map<String,Object> prop)
    throws IOException {
        final boolean packet;
        if (!prop.containsKey("method")) {
            packet = true;
        } else {
            final Object method = prop.get("method");
            if ("packet".equals(method)) packet = true;
            else if ("stream".equals(method)) packet = false;
            else throw new IOException("Illegal method `" + method + "'");
        }
        final boolean block = isTrue(prop.get("block"));
        
        Syslogger logger;
        Transport trans = null;
        TCPConf transConf = null;
        if (prop.containsKey("host")) {
            final String host = (String)prop.get("host");
            final int port = ((Integer)prop.get("port")).intValue();
            System.err.println("HOST: " + host + ", PORT: " + port);
            final InetSocketAddress addr = new InetSocketAddress(host, port);
            if (packet)
                logger = new UDPSyslogger(addr);
            else {
                final ByteFall buffer;
                if (block) {
                    trans = new TCPTransport(addr);
                    buffer = null;
                } else {
                    trans = new NBTCPTransport(addr);
                    buffer = new MemoryByteFall(
                        size("maxbuffer", (String)prop.get("maxbuffer"),
                                1*1024*1024));
                }
                transConf = (TCPConf)trans.configuration();
                logger = new ReusingTransportSyslogger(trans, buffer);
            }
        } else {
            final String path = (String)prop.get("path");
            if (packet) logger = new UnixDatagramSyslogger(path);
            else logger = new ReusingUnixStreamSyslogger(path);
        }
        
        for (Iterator it = prop.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)it.next();
            final String key = (String)entry.getKey();
            final Object value = entry.getValue();
            if ("host".equals(key) || "port".equals(key)
                || "path".equals(key) || "method".equals(key)
                    || "retries".equals(key) || "block".equals(key)) {
                //Avoid
            } else if ("priority".equals(key)) {
                final int priority
                    = PriorityUtils.parsePriority(value.toString());
                if (priority == -1) {
                    throw new IOException("Illegal priority " + value);
                }
                logger.setPriority(priority);
            } else if ("tag".equals(key)) {
                logger.setTag(value.toString());
            } else if ("hostname".equals(key)) {
                logger.setHost(value.toString());
            } else if ("terminator".equals(key)) {
                logger.setTerminator(value.toString());
            } else if ("packetlimit".equals(key)) {
                try {
                    final int limit = Integer.parseInt(value.toString());
                    logger.setPacketLimit(limit);
                } catch (NumberFormatException e) {
                    throw new IOException(
                        "Illegal limit `" + value + "'"
                        + " for option `packetlimit'");
                }
            } else if ("senddate".equals(key)) {
                logger.enableSendTimestamp(isTrue(value));
            } else if ("sendhost".equals(key)) {
                logger.enableSendHost(isTrue(value));
            } else if ("keepalive".equals(key)) {
                transConf.setKeepAlive(isTrue(value));
            } else if ("buffersize".equals(key) && transConf != null) {
                transConf.setSendBufferSize(nat(key, value.toString()));
            } else if ("linger".equals(key) && transConf != null) {
                transConf.setLinger(nat(key, value.toString()));
            } else if ("nodelay".equals(key) && transConf != null) {
                transConf.setNoDelay(isTrue(value));
            } else {
                throw new IOException(
                    "Unknown parameter `" + key + "'"
                    + " or illegal for this kind of connection");
            }
        }
        
        if (prop.containsKey("retries")) {
            final Object timesobj = prop.get("retries");
            if (timesobj == null) {
                logger = new RetryingSyslogger(logger);
            } else {
                try {
                    final int times = Integer.parseInt(timesobj.toString());
                    logger = new RetryingSyslogger(logger, times, null);
                } catch (NumberFormatException e) {
                    throw new IOException("Illegal retries `" + timesobj +"'");
                }
            }
        }
        
        return logger;
    }
    
    private static boolean isTrue(final Object value) {
        if (value == null) return true;
        final String str = value.toString().toLowerCase();
        return "yes".equals(str) || "on".equals(str) || "true".equals(str);
    }

    private static int nat(final String key, final String strvalue)
    throws IOException {
        try {
            final int value = Integer.parseInt(strvalue);
            if (value >= 0) return value;
        } catch (Exception e) {}
        throw new IOException(
            "Illegal limit `" + strvalue + "' for parameter `" + key + "'");
    }
    
    private static int size(final String key, final String strvalue,
            final int defaultSize)
    throws IOException {
        if (strvalue == null) return defaultSize;
        try {
            return (int)Unit.parseSuffix(Unit.ALLSIZE, strvalue);
        } catch (Exception e) {
            throw new IOException(
                "Illegal size value `" + strvalue
                + "' for parameter `" + key + "': " + e.getMessage(), e);
        }
    } 
    
    protected int getDefaultPort() {
        return 514;
    }
}
