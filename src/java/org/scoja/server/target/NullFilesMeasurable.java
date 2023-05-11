/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2008  Bankinter, SA.
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

import java.util.List;
import org.scoja.server.core.CPUUsage;
import org.scoja.server.core.Measure;

public class NullFilesMeasurable implements FilesMeasurable {

    private static final NullFilesMeasurable instance
        = new NullFilesMeasurable();
        
    public static NullFilesMeasurable getInstance() { return instance; }

    public void written(final String filename, final boolean opened, 
                        final int events, final int bytes,
                        final CPUUsage cpu) {}
    
    public Measure.Key getMeasureKey() {
        throw new UnsupportedOperationException();
    }
    
    public void stats(final List<Measure> measures) {}
}
