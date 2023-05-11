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

package org.scoja.server.source;

import org.scoja.common.PriorityUtils;
import org.scoja.server.core.ScojaThread;
import org.scoja.server.core.ClusterSkeleton;
import org.scoja.server.core.DecoratedLink;
import org.scoja.server.core.Event;
import org.scoja.server.core.EventContext;
import org.scoja.server.core.Link;
import org.scoja.server.core.Linkable;
import org.scoja.server.core.Environment;
import org.scoja.server.core.HubMeasurable;
import org.scoja.server.core.InternalEvent;
import org.scoja.server.core.Measurable;
import org.scoja.server.core.Measure;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

public class Measurer 
    extends ClusterSkeleton
    implements DecoratedLink, Runnable {

    protected final InetAddress localhost;    
    protected final Link link;
    protected final HubMeasurable meas;
    protected final long reference;
    protected long duration;
    
    public Measurer() throws UnknownHostException {
        this.localhost = InetAddress.getLocalHost();
        this.link = new Link();
        this.meas = new HubMeasurable();
        this.reference = dayStart();
        this.duration = 5*60*1000;
        super.setThreads(1);
    }
    
    public Linkable getLinkable() {
        return link;
    }
    
    public void addTarget(final Linkable next) {
        link.addTarget(next);
    }
    
    public void removeTarget(final Linkable next) {
        link.removeTarget(next);
    }
    
    public void addMeasurable(final Measurable mea) {
        meas.addMeasurable(mea);
    }
    
    public void removeMeasurable(final Measurable mea) {
        meas.removeMeasurable(mea);
    }
    
    public void setPeriod(final long duration) {
        this.duration = duration;
    }
    
    public void start() {
        Internal.warning(Internal.SOURCE_MEASURER, "Starting " + this);
        super.start();
        super.startAllThreads();
    }
    
    public void run() {
        while (!stopRequested()) {
            waitNextPeriod();
            collectProcess();
        }
    }
    
    protected long dayStart() {
        final Calendar now = Calendar.getInstance();
        return new GregorianCalendar(
            now.get(Calendar.YEAR), 
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH))
            .getTimeInMillis();
    }
    
    protected void waitNextPeriod() {
        final long now = System.currentTimeMillis();
        final long periods = (now - reference + duration - 1) / duration;
        final long tosleep = reference + duration*periods - now;
        try {
            Thread.sleep(tosleep);
        } catch (InterruptedException shouldBeIgnored) {}
    }
    
    protected void collectProcess() {
        final Thread thread = Thread.currentThread();
        if (!(thread instanceof ScojaThread)) {
            Internal.emerg(Internal.SOURCE_MEASURER,
                           "Refusing to be executed by thread " + thread 
                           + ": it isn't a ScojaThread.");
            return;
        }
        final ScojaThread sthread = (ScojaThread)thread;
        
        //Internal.info(Internal.SOURCE_MEASURER,
        //        "Starting measures generationg");
        final List<Measure> ms = new ArrayList<Measure>();
        meas.stats(ms);
        Collections.sort(ms, Measure.ascending);
        Measure.Key prevKey = null;
        EventContext ectx = null;
        Environment env = null;
        for (final Iterator<Measure> it = ms.iterator(); it.hasNext(); ) {
            final Measure current = it.next();
            if (!current.getKey().equals(prevKey)) {
                if (ectx != null) process(sthread, ectx);
                prevKey = current.getKey();
                final Event event = new InternalEvent(
                    localhost, Event.DEFAULT_ENERGY,
                    PriorityUtils.SYSLOG, PriorityUtils.INFO,
                    "syslog.scoja.stats", "");
                ectx = new EventContext(event);
                env = ectx.getEnvironment();
                env.unknown("");
                env.define("kind", prevKey.getKind());
                env.define("subkind", prevKey.getSubkind());
                env.define("parameters", prevKey.getParameters());
            }
            env.define("partial-" + current.getConcept(),
                       current.getPartialAsString());
            env.define("total-" + current.getConcept(),
                       current.getTotalAsString());
        }
        if (prevKey != null) process(sthread, ectx);
        //Internal.info(Internal.SOURCE_MEASURER,
        //        "Ending measures generationg");
    }
    
    protected void process(final ScojaThread sthread, final EventContext ectx){
        sthread.setEventContext(ectx);
        link.process(ectx);
    }

    public String toString() {
        return "Measurer source";
    }
    
    public String getName4Thread() {
        return "measurer";
    }
}
