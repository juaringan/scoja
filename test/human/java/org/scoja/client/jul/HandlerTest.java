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
package org.scoja.client.jul;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HandlerTest {

    protected static final Logger log
        = Logger.getLogger(HandlerTest.class.getName());
    
    public static void main(final String[] args) {
        if (args.length == 0) {
            System.out.println(
                "Run me with: "
                + "\n  option -Djava.protocol.handler.pkgs=org.scoja.protocol,"
                + "\n    so that I can understand `syslog' protocol,"
                + "\n  option -Dorg.scoja.io.posix.provider"
                + "=org.scoja.io.posix.PosixNative"
                + "\n    if you want to log locally with an unix socket,"
                + "\n  option -Djava.util.logging.config.file=<conffile>"
                + "\n    with a properties configuration file,"
                + "\n  and a dummy argument to avoid this message."
                );
        } else {
            log.info("Test message");
            log.log(Level.INFO, "Test message with arguments",
                    new Object[] {"a1", "a2"});
            log.info("was.activity:Activity message from was");
            log.info("was.connection:Connection message from was");
        }
    }
}
