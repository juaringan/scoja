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

package org.scoja.server.conf;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.python.core.PyModule; 
import org.python.core.adapter.PyObjectAdapter; 
import org.python.core.adapter.ClassicPyObjectAdapter; 
import org.python.util.PythonInterpreter; 

import org.scoja.cc.lang.Void;
import org.scoja.common.ConfigurationException;

public class ScojaConfiguration extends AtomicFileConfiguration {

    private static final String PRECONF_FILE = "scoja_conf.py";

    public ScojaConfiguration(final GlobalContext globalContext,
                              final String filename)
        throws IOException {
        super(globalContext, filename);
    }
    
    protected ConfigurationSoul load()
    throws ConfigurationException {
        final PyObjectAdapter adapter = new ClassicPyObjectAdapter();
        final PythonInterpreter interp = new PythonInterpreter();
        final Executor exec = new Executor(interp);

        final PyModule scojaMod = new PyModule("scoja", null);
        interp.set("scoja", scojaMod);
        scojaMod.__setattr__("executor", adapter.adapt(exec));
        final ConfigurationSoul soul = new ConfigurationSoul(globalContext);
        scojaMod.__setattr__("soul", adapter.adapt(soul));
        final File src = file.getFile();
        scojaMod.__setattr__("basedir", adapter.adapt(src.getParentFile()));

        final InputStream preconf
            = getClass().getResourceAsStream(PRECONF_FILE);
        if (preconf == null) {
            throw new ConfigurationException(
                "Cannot find file with native Scoja Language definition"
                + " (" + PRECONF_FILE + ")");
        }
        try {
            try {
                interp.execfile(preconf, "<scoja language definition>");
            } finally {
                preconf.close();
            }
        } catch (Throwable e) {
            throw new ConfigurationException(
                "Error while loading native Scoja Language definition"
                + "\": " + e.toString().trim(), e);
        }
        
        try {
            exec.execfile(file.getFile());
        } catch (Throwable e) {
            /*
            if (e instanceof org.python.core.PyException) {
                final java.io.PrintWriter out
                    = new java.io.PrintWriter(System.err);
                ((org.python.core.PyException)e)
                    .super__printStackTrace(out);
                out.flush();
            } else e.printStackTrace(System.err);
            */
            throw new ConfigurationException(
                "Error while interpreting configuration file "
                + "\"" + file.getFile() + "\": " + e.toString().trim(), e);
        }
        
        return soul;
    }
    
    
    //======================================================================
    public static class Executor {
    
        protected final PythonInterpreter interp;
        
        public Executor(final PythonInterpreter interp) {
            this.interp = interp;
        }
    
        public void execfile(final File filename)
        throws Exception {
            AccessController.doPrivileged(
                new PrivilegedExceptionAction<Void>() {
                public Void run() throws Exception {
                    final InputStream is = new FileInputStream(filename);
                    try {
                        interp.execfile(is, filename.toString());
                    } finally {
                        is.close();
                    }
                    return Void.ME;
                }
            });
        }
    }
}
