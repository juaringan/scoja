/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2007  Mario Martínez
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

import java.io.PrintWriter;
import java.util.Calendar;
import java.net.InetAddress;
import java.security.Principal;

import org.scoja.cc.ldap.DistinguishedName;

/**
 * Representa un evento de Syslog.
 * Es una interfaz para poder tener eventos externos que hay que
 * parsear y eventos internos que ya se construyen con sus datos.
 */
public interface Event {

    public static final int NO_ENERGY = 0;
    public static final int DEFAULT_ENERGY = 1;
    public static final long UNKNOWN_TIMESTAMP = 0;

    public int getEnergy();    
    
    public int getByteSize();
    
    public int getPriority();
    
    public String getPriorityName();
    
    public int getFacility();
    
    public String getFacilityName();
    
    public int getLevel();
    
    public String getLevelName();
    
    public void chooseReceptionAsPreferredTimestamp(boolean choose);
    
    public long getPreferredTimestamp();
    
    public Calendar getPreferredCalendar();
    
    public long getSendTimestamp();
    
    public Calendar getSendCalendar();
    
    public long getReceptionTimestamp();
    
    public Calendar getReceptionCalendar();
    
    /**
     * Devuelve el nombre que venga en el evento,
     * o <code>getAddress().getHostAddress()</code>.
     */
    public String getHost();
    
    public QStr getQHost();
    
    /**
     * Si el evento tiene host, calcula su nombre canónico.
     * Si no, devuelve <code>getCanonicalHostName()</code>.
     */
    public String getCanonicalHost();
    
    public QStr getQCanonicalHost();
    
    /**
     * Devuelve la IP del origen del evento (localhost para los
     * paquetes internos y locales).
     */
    public InetAddress getAddress();
    
    public QStr getQAddress();
    
    /**
     * Devuelve el <code>getAddress().getHostName()</code>.
     */
    public String getHostName();
    
    public QStr getQHostName();
    
    /**
     * Devuelve el <code>getAddress().getCanonicalHostName()</code>.
     */
    public String getCanonicalHostName();
    
    public QStr getQCanonicalHostName();
    
    /**
     * Devuelve la parte de datos del evento, que en un paquete de
     * syslog convencional es
     * <code>getProgram() + ": " + getMessage()</code>.
     */
    public String getData();
    
    public QStr getQData();
    
    /**
     * Devuelve el programa que ha originado este evento.
     */
    public String getProgram();
    
    public QStr getQProgram();
    
    /**
     * Devuelve el mensaje del evento.
     */
    public String getMessage();
    
    public QStr getQMessage();

    /**
     * Client identification.
     */
    public Principal getPeerPrincipal();
    
    public QStr getQPeerPrincipal();
    
    public DistinguishedName getPeerDN();
        
    /**
     * Escribe el evento a <code>out</code>, con el formato estándar
     * de syslog.
     */
    public void writeTo(final PrintWriter out);
}
