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

package org.scoja.client.jul;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;

public class Composite extends EventLayout {
    
    protected final EventLayout[] sub;
    
    public Composite(final EventLayout[] sub) {
        this.sub = sub;
    }
    
    public Composite(final List sub) {
        this((EventLayout[])sub.toArray(new EventLayout[0]));
    }
    
    public EventLayout simplified() {
        if (sub.length == 0) return new Literal("");
        if (sub.length == 1) return sub[0].simplified();
        final List result = new ArrayList();
        EventLayout last = sub[0].simplified();
        for (int i = 1; i < sub.length; i++) {
            final EventLayout current = sub[i].simplified();
            if ((last instanceof Literal) && (current instanceof Literal)) {
                last = ((Literal)last).append((Literal)current);
            } else {
                if (!last.isEmpty()) result.add(last);
                last = current;
            }
        }
        if (result.isEmpty()) return last;
        if (!last.isEmpty()) result.add(last);
        return new Composite(result);
    }
    
    public void format(final StringBuffer target, final LogRecord lr) {
        for (int i = 0; i < sub.length; i++) sub[i].format(target, lr);
    }
}
