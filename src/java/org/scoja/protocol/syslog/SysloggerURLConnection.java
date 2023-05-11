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

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import org.scoja.client.Syslogger;
import org.scoja.io.EmptyInputStream;

/**
 * This implements an URLConnection to a syslog server.
 * This connection can be used only to do output;
 * nevertheless, {@link #getInputStream()} returns a correct but empty
 * {@link InputStream}.
 * <p>
 * Syslog is such a trivial and specialized protocol that most of the
 * {@link URLConnection} capabilities has no sense.
 * Configuring <tt>doInput</tt>, <tt>doOutput</tt>,
 * <tt>allowUserInteraction</tt>, <tt>ifModifiedSince</tt>
 * has no use because they default to the only sensible values.
 * <tt>useCaches</tt> is pretty usefull, but this implementation currently
 * ignores it.
 */
public class SysloggerURLConnection
    extends URLConnection {
    
    protected final Syslogger logger;
    protected final OutputStream out;
    
    protected SysloggerURLConnection(final URL url, final Syslogger logger) {
        super(url);
        this.logger = logger;
        this.out = logger.newOutputStream();
    }
    
    public void connect() {}
    
    public int getContentLength() {
        return 0;
    }
    
    public InputStream getInputStream() {
        return EmptyInputStream.getInstance();
    }
    
    public OutputStream getOutputStream() {
        return out;
    }
    
    public Syslogger getLogger() {
        return logger;
    }
}
