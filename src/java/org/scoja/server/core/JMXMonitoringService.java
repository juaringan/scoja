/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2008  Mario Martínez
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

import java.lang.management.*;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.rmi.*;

import org.scoja.cc.rmi.FixedRMIClientSocketFactory;
import org.scoja.cc.rmi.PlainRMIServerSocketFactory;
import org.scoja.server.source.Internal;

/**
 * This is a Proxy to expose an JMXConnectorServer as a Cluster,
 * so that we can configure it on an standard Scoja configuration file.
 *
 * <p>
 * This cluster {@link #shouldStop} method <i>almost</i> work.
 * But if a {@link #start} is executed close to a {@link #shouldStop},
 * it sometimes fails with a <i>port already in use</i> error.
 * It seems that {@link JMXConnectorServer#stop}
 * and {@link UnicastRemoteObject#unexportObject}
 * don't close theirs sockets immediately.
 * Adding an sleep to {@link #shouldStop} solves this problem, but
 * introduces an innecesary delay to the reloading process.
 * We prefer to suffer the error, because in practice reloading a configuration
 * means to stop and start many clusters, and the necesary delay will be
 * added by other clusters.
 */
public class JMXMonitoringService implements Cluster {

    protected Registry registry;
    protected JMXConnectorServer server;
    protected String ip;
    protected int rport;
    protected int sport;
    protected String passwordFile;
    protected String accessFile;
    
    public JMXMonitoringService() {
        this.registry = null;
        this.server = null;
        this.ip = "0.0.0.0";
        this.rport = 1599;
        this.sport = 1598;
        this.passwordFile = null;
        this.accessFile = null;
    }
    
    public void setIp(final String ip) {
        this.ip = ip;
    }
    
    public void setRegistryPort(final int rport) {
        this.rport = rport;
    }
    
    public void setServerPort(final int sport) {
        this.sport = sport;
    }
    
    public void setPasswordFile(final String passwordFile) {
        this.passwordFile = passwordFile;
    }
    
    public void setAccessFile(final String accessFile) {
        this.accessFile = accessFile;
    }

    public synchronized boolean isRunning() {
        return server != null;
    }
    
    public synchronized void start() {
        if (isRunning()) return;
        Internal.warning(Internal.MONITOR, "Starting " + this);
        try {
            final HashMap<String,Object> env = new HashMap<String,Object>();
            env.put("jmx.remote.x.password.file", passwordFile);
            env.put("jmx.remote.x.access.file", accessFile);
            final PlainRMIServerSocketFactory ssf
                = new PlainRMIServerSocketFactory();
            ssf.setAddress(ip);
            ssf.setReuseAddress(true);
            final FixedRMIClientSocketFactory csf
                = new FixedRMIClientSocketFactory(ip);
            registry = LocateRegistry.createRegistry(rport, csf, ssf);
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final String urlstr
                = "service:jmx:rmi://" + ip + ":" + sport + "/jndi/"
                + "rmi://" + ip + ":" + rport + "/jmxrmi";
            System.err.println(
                "URI: " + urlstr
                + ", password file: " + passwordFile
                + ", access file: " + accessFile);
            final JMXServiceURL url = new JMXServiceURL(urlstr);
            server = JMXConnectorServerFactory
                .newJMXConnectorServer(url,env,mbs);
            server.start();
        } catch (Exception e) {
            Internal.err(Internal.MONITOR,
                         "Cannot start monitoring service.", e);
            //e.printStackTrace(System.err);
            try {
                destroy();
            } catch (Exception ignored) {}
        }
    }
    
    public synchronized void shouldStop() {
        if (!isRunning()) return;
        Internal.warning(Internal.MONITOR, "Stopping " + this);
        try {
            destroy();
            //With this wait, the following start never fails.
            //Thread.sleep(500);
        } catch  (Exception e) {
            Internal.err(Internal.MONITOR, "Cannot stop monitoring", e);
        } 
    }
    
    protected void destroy()
    throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
        if (registry != null) { 
            UnicastRemoteObject.unexportObject(registry,true);
            registry = null;
        }
    }
    
    public int getThreads() {
        return 1;
    }
    
    public void setThreads(final int max) {
        throw new UnsupportedOperationException();
    }
    
    public int getCurrentThreads() {
        return 1;
    }
    
    public String toString() {
        return "JMX server monitor at " + ip + ":" + rport + "/" + sport;
    }
}
