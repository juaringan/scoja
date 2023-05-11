
package org.scoja.server.core;

import java.util.Calendar;
import java.net.InetAddress;
import java.security.Principal;
import javax.security.auth.x500.X500Principal;

import org.scoja.cc.ldap.DistinguishedName;
import org.scoja.common.DateUtils;
import org.scoja.common.PriorityUtils;

/**
 * Es un paso intermedio en la implementación de {@link Event}.
 */
public abstract class EventSkeleton implements Event {

    protected final long buildTimestamp;
    protected Calendar buildCalendar;
    protected String asString;
    
    protected QStr qHost;
    protected QStr qAddress;
    protected QStr qHostName;
    protected QStr qCanonicalHostName;
    protected QStr qData;
    protected QStr qProgram;
    protected QStr qMessage;
    protected QStr qPeerPrincipal;
    protected DistinguishedName peerDN;
    protected boolean peerDNDone;

    public EventSkeleton() {
        this(System.currentTimeMillis());
    }
    
    public EventSkeleton(final long buildTimestamp) {
        this.buildTimestamp = buildTimestamp;
        this.buildCalendar = null;
        this.asString = null;
        this.qHost = null;
        this.qAddress = null;
        this.qHostName = null;
        this.qCanonicalHostName = null;
        this.qData = null;
        this.qProgram = null;
        this.qMessage = null;
        this.qPeerPrincipal = null;
        this.peerDN = null;
        this.peerDNDone = false;
    }

    public int getEnergy() {
        return DEFAULT_ENERGY;
    }
    
    public String getPriorityName() {
        return PriorityUtils.getPriorityName(getPriority());
    }
    
    public int getFacility() {
        return PriorityUtils.getFacility(getPriority());
    }

    public String getFacilityName() {
        return PriorityUtils.getFacilityName(getFacility());
    }
        
    public int getLevel() {
        return PriorityUtils.getLevel(getPriority());
    }
    
    public String getLevelName() {
        return PriorityUtils.getLevelName(getLevel());
    }
    
    public boolean isTraceable() {
        return true;
    }
    
    public boolean shouldLogErrors() {
        return true;
    }
    
    public void chooseReceptionAsPreferredTimestamp(final boolean choose) {
    }
    
    public long getPreferredTimestamp() {
        return getReceptionTimestamp();
    }
    
    public Calendar getPreferredCalendar() {
        return getReceptionCalendar();
    }
    
    public long getSendTimestamp() {
        return getReceptionTimestamp();
    }
    
    public Calendar getSendCalendar() {
        return getReceptionCalendar();
    }
    
    public long getReceptionTimestamp() {
        return buildTimestamp;
    }
    
    public Calendar getReceptionCalendar() {
        if (buildCalendar == null) {
            buildCalendar = Calendar.getInstance();
            buildCalendar.setTimeInMillis(buildTimestamp);
        }
        return buildCalendar;
    }
    
    public String getHostName() {
        return getAddress().getHostName();
    }
    
    public String getCanonicalHostName() {
        return getAddress().getCanonicalHostName();
    }

    public QStr getQHost() {
        //FIXME: Fix the parser so that it is sure that host has no eoln.
        if (qHost == null) {
            qHost = new QStr(getHost());
        }
        return qHost;
    }
    
    public QStr getQAddress() {
        if (qAddress == null) {
            qAddress = new QStr(getAddress().getHostAddress(),
                                QStr.IS_FILENAME_SECURE | QStr.HASNT_EOLN);
        }
        return qAddress;
    }
        
    public QStr getQHostName() {
        if (qHostName == null) {
            qHostName = new QStr(getHostName(), QStr.HASNT_EOLN);
        }
        return qHostName;
    }
    
    public QStr getQCanonicalHostName() {
        if (qCanonicalHostName == null) {
            qCanonicalHostName = new QStr(getCanonicalHostName(),
                                          QStr.HASNT_EOLN);
        }
        return qCanonicalHostName;
    }
    
    public QStr getQData() {
        if (qData == null) {
            qData = new QStr(getData());
        }
        return qData;
    }
    
    public QStr getQProgram() {
        //FIXME: Fix the parser so that it is sure to program has no EOLN.
        if (qProgram == null) {
            qProgram = new QStr(getProgram());
        }
        return qProgram;
    }
    
    public QStr getQMessage() {
        if (qMessage == null) {
            qMessage = new QStr(getMessage());
        }
        return qMessage;
    }
        
    public QStr getQPeerPrincipal() {
        if (qPeerPrincipal == null) {
            final Principal p = getPeerPrincipal();
            qPeerPrincipal
                = (p == null) ? Environment.Q_UNKNOWN : new QStr(p.getName());
        }
        return qPeerPrincipal;
    }
    
    public DistinguishedName getPeerDN() {
        if (!peerDNDone) {
            peerDNDone = true;
            final Principal pp = getPeerPrincipal();
            if (pp instanceof X500Principal) {
                try {
                    peerDN = new DistinguishedName(pp.getName());
                } catch (Throwable e) {
                    //If cannot be parsed, then we have no DN info
                }
            }
        }
        return peerDN;
    }
    
    //======================================================================
    public String toString() {
        if (asString == null) {
            asString = "Event"
                + "[energy: " + getEnergy()
                + ", priority: " + getPriorityName()
                + ", send: " + DateUtils.formatGMT(getSendCalendar())
                + ", received: " + DateUtils.formatGMT(getReceptionCalendar())
                + ", host: " + getHost()
                + ", data: \"" + getData() + "\""
                + ", program: \"" + getProgram() + "\""
                + ", message: \"" + getMessage() + "\""
                + "]";
        }
        return asString;
    }
    
}
