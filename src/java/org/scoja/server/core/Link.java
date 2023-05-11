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

package org.scoja.server.core;


/**
 * Es la clase base de todos los elementos que tratan con
 * {@link Event}s.
 * Vale tanto para los elementos que producen, como los que filtran o
 * escriben; todo es igual, y todos pueden hacer cosas sobre los
 * elementos que los atraviesan.
 * La funcionalidad que se implementa a este nivel es la distribución
 * de los eventos producidos a todos los {@link Link}s que se hayan
 * registrado.
 */
public class Link
    extends FullLinkAtPython
    implements Linkable, DecoratedLink {

    protected EventQueue queue;
    protected Link[] targets;
    protected int used;
    
    public Link() {
        this.queue = null;
        this.targets = new Link[5];
        this.used = 0;
    }
    
    public void setQueue(final EventQueue queue) {
        this.queue = queue;
    }
    
    public void removeQueue() {
        this.queue = null;
    }
    
    public int getTargetSize() {
        return used;
    }
    
    public void removeTarget(final Linkable target) {
        int pos = 0;
        while (pos < used && targets[pos] != target) pos++;
        if (pos < used) {
            used--;
            for (int i = pos; i < used; i++) targets[i] = targets[i+1];
        }
    }
    
    public void addTarget(final Linkable target) {
        target.addSimpleSource(this);
    }
    
    public void addSimpleTarget(final Link target) {
        if (used >= targets.length) {
            final Link[] newTargets = new Link[2*targets.length];
            for (int i = 0; i < targets.length; i++) {
                newTargets[i] = targets[i];
            }
            targets = newTargets;
        }
        targets[used++] = target;
    }
    
    public void addSimpleSource(final Link source) {
        source.addSimpleTarget(this);
    }

    public Linkable getLinkable() {
        return this;
    }    
            
    public void process(final EventContext env) { 
        //To make it works in a concurrent context
        final EventQueue q = queue;
        if (q != null) {
            env.setLastLink(this);
            q.process(env);
        } else {
            propagate(env);
        }
    }
    
    public void propagate(final EventContext ectx) {
        //System.out.println("Propagating " + ectx + " to " + targets.size());
        for (int i = 0; i < used && !ectx.hasCompleted(); i++) {
            targets[i].process(ectx);
        }
    }

}
