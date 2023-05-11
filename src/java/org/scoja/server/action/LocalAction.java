
package org.scoja.server.action;

import org.scoja.common.PriorityUtils;
import org.scoja.server.core.Link;
import org.scoja.server.core.Linkable;
import org.scoja.server.core.EventContext;
import org.scoja.server.source.Internal;

public class LocalAction extends Link {

    public void propagate(final EventContext env) {
        env.getEnvironment().mark();
        if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
            Internal.debug(env, Internal.ACTION,
                           "Set a mark at environment of " + env);
        }
        super.propagate(env);
        env.getEnvironment().release();
        if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
            Internal.debug(env, Internal.ACTION,
                           "Removed a mark at environment of " + env);
        }
    }
    
    public Linkable getLinkable() {
        return new LocalAction();
    }    
}
