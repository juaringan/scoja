
package org.scoja.server.core;

import org.scoja.server.filter.Filter;
import org.scoja.server.filter.FilteredLink;

public abstract class FullLinkAtPython 
    extends LinkableAtPython
    implements DecoratedLink {

    public Linkable __or__(final DecoratedLink dtarget) {
        final Linkable link = this.getLinkable();
        final Linkable target = dtarget.getLinkable();
        return new JoinedLink(link, target);
    }
}
