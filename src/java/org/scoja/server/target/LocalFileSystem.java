/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2008  Bankinter, S.A.
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

import org.scoja.server.source.Internal;

public class LocalFileSystem extends BinaryFileSystem {

    public BinaryFile openBinary(final String filename,
                                 final FileBuilding building) {
        return new PlainFile(this, filename, building);
    }
    
    protected long error(final String filename, final Exception e) {
        Internal.warning(Internal.TARGET_FILE,
                         "Error while managing `" + filename + "': "
                         + e.getMessage());
        return System.currentTimeMillis() + inactivityAfterError;
    }
}    
