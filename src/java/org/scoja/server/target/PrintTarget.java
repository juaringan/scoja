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

package org.scoja.server.target;

import org.scoja.server.core.EventContext;
import org.scoja.server.core.Link;
import org.scoja.server.template.EventWriter;
import org.scoja.server.template.Template;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.OutputStream;

public class PrintTarget extends Link {

    //======================================================================
    private static PrintTarget toStdOut = null;
    private static PrintTarget toStdErr = null;
    
    public static synchronized PrintTarget getStdOutInstance() {
        if (toStdOut == null) {
            toStdOut = new PrintTarget(System.out);
        }
        return toStdOut;
    }

    public static synchronized PrintTarget getStdErrInstance() {
        if (toStdErr == null) {
            toStdErr = new PrintTarget(System.err);
        }
        return toStdErr;
    }

    //======================================================================
    protected final PrintWriter out;
    protected EventWriter writer;
    protected Flushing flushing;
    protected int delayed;

    public PrintTarget(final PrintWriter out) {
        this.out = out;
        this.writer = EventWriter.Standard.getInstance();
        this.flushing = Flushing.getDefault();
        this.delayed = 0;
    }
    
    public PrintTarget(final Writer out) {
        this(new PrintWriter(out));
    }
    
    public PrintTarget(final OutputStream out) {
        this(new PrintWriter(out));
    }

    public void setFormat(final Template writer) {
        this.writer = writer;
    }
    
    public void setFlushing(final Flushing flushing) {
        this.flushing = (flushing == null) ? Flushing.getDefault() : flushing;
    }

    public void process(final EventContext ectx) {
        synchronized (out) {
            writer.writeTo(out, ectx);
        }
        flush();
        super.process(ectx);
    }
    
    private synchronized void flush() {
        delayed++;
        if (flushing.getMethod() != Flushing.BUFFER
            && delayed >= flushing.getAllowedDelay()) {
            out.flush();
            delayed = 0;
        }
    }
    
}
