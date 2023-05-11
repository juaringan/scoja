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

package org.scoja.server.template;

import junit.framework.*;
import junit.textui.TestRunner;

import java.io.*;
import java.util.Calendar;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.scoja.common.PriorityUtils;
import org.scoja.server.core.Event;
import org.scoja.server.core.InternalEvent;
import org.scoja.server.core.EventContext;

/**
 */
public class TemplateTest extends TestCase {

    public static void main(final String args[]) {
        TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(TemplateTest.class);
    }
    
    public TemplateTest(final String name) {
        super(name);
    }
    
    protected static final String ip = "127.0.0.1";
    protected static final String program = "test";
    protected static final String message = "how are you?";
    protected static final int facility = PriorityUtils.LOCAL0;
    protected static final int level = PriorityUtils.ERR;
    protected static final int priority
        = PriorityUtils.buildPriority(facility, level);
    
    protected Calendar cal;
    protected EventContext ectx;
    
    protected void setUp() throws Exception {
        cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, 8, 14, 1, 11, 2);
        final Event event
            = new InternalEvent(cal.getTimeInMillis(),
                                InetAddress.getByName(ip),
                                1, priority, program, message);
        ectx = new EventContext(event);
    }

    protected void tearDown() {
    }

    public void testSimplification() throws UnknownHostException {
        final InetAddress localhost = InetAddress.getLocalHost();
        final Template ot1 = Template.parse(
            "Verbatim1 $$ ${ME} ${MYIP} ${MYHOST} ${MYCHOST}");
        final Template st1 = new Template(new TemplatePart[] {
            new VerbatimPart(
                "Verbatim1 $"
                /*${ME}*/ + " SCOJATEST"
                /*${MYIP}*/ + " " + localhost.getHostAddress()
                /*${MYHOST}*/ + " " + localhost.getHostName()
                /*${MYCHOST}"*/ + " " + localhost.getCanonicalHostName()),
        });
        System.out.println("Simplified " + ot1.simplified());
        assertEquals(st1, ot1.simplified());
        
        final Template ot2 = Template.parse(
            "Verbatim1 ${X1}${X2} $$ ${X3}$$");
        final Template st2 = new Template(new TemplatePart[] {
            new VerbatimPart("Verbatim1 "),
            new VarHole("X1"),
            new VarHole("X2"),
            new VerbatimPart(" $ "),
            new VarHole("X3"),
            new VerbatimPart("$"),
        });
        System.out.println("Simplified " + ot2.simplified());
        assertEquals(st2, ot2.simplified());
    }
    
    /**
     */
    public void testPredefinedForFilename() {
        final Template temp
            = Template.parse("Template"
                             + " ${FACILITY} ${LEVEL} ${PRIORITY}"
                             + " ${FACILITY#} ${LEVEL#} ${PRIORITY#}"
                             + " ${IP} ${RHOST} ${CRHOST} ${HOST} ${CHOST}"
                             + " ${YEAR} ${_YEAR}"
                             + " ${MONTH} ${_MONTH}"
                             + " ${MONTHNAME} ${_MONTHNAME}"
                             + " ${DAY} ${_DAY}"
                             + " ${HOUR} ${_HOUR}"
                             + " ${MINUTE} ${_MINUTE} ${MIN} ${_MIN}"
                             + " ${SECOND} ${_SECOND} ${SEC} ${_SEC}"
                             + " ${MILLISECOND} ${_MILLISECOND}"
                             +   " ${MILLIS} ${_MILLIS}"
                             + " ${EPOCH} ${MILLIEPOCH}"
                             + "");
        final String shouldBe
            = "Template"
            + " local0 err local0.err"
            + " " + facility + " " + level + " " + priority
            + " 127.0.0.1 localhost localhost localhost localhost"
            + " 2003 03"
            + " 09 9"
            + " September Sep"
            + " 14 14"
            + " 01 1"
            + " 11 11 11 11"
            + " 02 2 02 2"
            + " 000 0 000 0"
            + " " + (cal.getTimeInMillis()/1000) + " " + cal.getTimeInMillis()
            + "";
        final String filename = temp.toFilename(ectx);
        final String written = written(temp, ectx);
        assertEquals(filename, shouldBe);
        assertEquals(written, shouldBe);
    }
    
    public void testPredefinedToPrint() {
        final Template temp
            = Template.parse("Template"
                             + " ${DATA} ${PROGRAM} ${MESSAGE}"
                             + "");
        final String shouldBe
            = "Template"
            + " " + program + ": " + message + " " + program + " " + message
            + "";
        final String written = written(temp, ectx);
        assertEquals(written, shouldBe);
    }

    protected String written(final Template temp, final EventContext ectx) {
        final CharArrayWriter writer = new CharArrayWriter();
        final PrintWriter out = new PrintWriter(writer);
        temp.writeTo(out, ectx);
        out.flush();
        return writer.toString();
    }    
}
