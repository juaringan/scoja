
package org.scoja.server.action;

import org.scoja.server.core.Linkable;
import org.scoja.server.core.FullLinkAtPython;

public abstract class ActionLinkableAtPython
    extends FullLinkAtPython
    implements Action {

    public Linkable getLinkable() {
        return new ActionLink(this);
    }
}
