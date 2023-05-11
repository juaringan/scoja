
package org.scoja.server.filter;

import org.scoja.server.core.Link;
import org.scoja.server.core.EventContext;

import org.scoja.server.source.Internal;
import org.scoja.common.PriorityUtils;

public class FilteredLink extends Link {
    
    protected final Filter filter;
    
    public FilteredLink(final Filter filter) {
        this.filter = filter;
    }
    
    public void process(final EventContext env) {
        if (filter.isGood(env)) {
            if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
                Internal.debug(env, Internal.FILTER,
                               "Event " + env + " passed " + filter);
            }
            super.process(env);
        } else {
            if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
                Internal.debug(env, Internal.FILTER,
                               "Event " + env + " didn't pass " + filter);
            }
        }
    }
    
    public String toString() {
        return "Link " + filter.toString();
    }
}
