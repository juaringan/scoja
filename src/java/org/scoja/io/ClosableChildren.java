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

package org.scoja.io;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class ClosableChildren {

    protected final Set children;
    protected boolean childrenClosed;
    
    public ClosableChildren() {
        this.children = new HashSet();
        this.childrenClosed = false;
    }

    protected void addChild(final Closable child)
    throws IOException {
        synchronized (children) {
            if (!childrenClosed) {
                children.add(child);
                return;
            }
        }
        child.close();
    }
    
    protected void removeChild(final Closable child) {
        synchronized (children) {
            //Conditional execution of remove is fundamental because
            //  closeChildren loops over children out of critical section.
            if (!childrenClosed) children.remove(child);
        }
    }
    
    protected void closeAll()
    throws IOException {
        synchronized (children) {
            if (childrenClosed) return;
            childrenClosed = true;
        }
        final Iterator it = children.iterator();
        IOException firstException = null;
        while (it.hasNext()) {
            try {
                ((Closable)it.next()).close();
            } catch (IOException e) {
                if (firstException == null) firstException = e;
            }
        }
        children.clear();
        if (firstException != null) throw firstException;
    }
}
