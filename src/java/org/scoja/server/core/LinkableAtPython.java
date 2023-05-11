
package org.scoja.server.core;

import org.scoja.server.filter.Filter;
import org.scoja.server.filter.FilteredLink;

public abstract class LinkableAtPython implements DecoratedLink {

    public Linkable __rshift__(final DecoratedLink dtarget) {
        final Linkable source = this.getLinkable();
        final Linkable target = dtarget.getLinkable();
        source.addTarget(target);
        return new ChainedLink(source,target);
    }
}
