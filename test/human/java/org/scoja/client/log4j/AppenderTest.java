/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003-2005  Mario Martínez
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
package org.scoja.client.log4j;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

public class AppenderTest {

    public static void main(final String[] args)
    throws IOException {
        if (args.length == 0) {
            System.out.println(
                "Run me with: "
                + "\n  option -Djava.protocol.handler.pkgs=org.scoja.protocol,"
                + "\n    so that I can understand `syslog' protocol,"
                + "\n  option -Dorg.scoja.io.posix.provider"
                + "=org.scoja.io.posix.PosixNative"
                + "\n    if you what to log locally with an unix socket,"
                + "\n  and an argument with a properties configuration file."
                );
            System.exit(1);
        }
        final String confFile = args[0];
        if (confFile.endsWith(".xml")) {
            DOMConfigurator.configure(confFile);
        } else {
            PropertyConfigurator.configure(confFile);
        }
        final Logger log = Logger.getLogger(AppenderTest.class.getName());
        MDC.put("env", "dev");
        MDC.put("app", "example");
        log.info("Logging all the arguments");
        for (int i = 0; i < args.length; i++)
            log.info("Argument " + i + ": " + args[i]);
        log.info("Logging stdin");
        final BufferedReader in
            = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) log.info(line);
    }
}
