
package org.scoja.server.filter;

import org.scoja.server.core.EventContext;

public class NotFilter extends FilterLinkableAtPython {

    protected final Filter negated;

    public NotFilter(final Filter negated) {
        this.negated = negated;
    }

    public boolean isGood(final EventContext env) {
        return !negated.isGood(env);
    }
    
    public String toString() {
        return "~(" + negated + ")";
    }
}
