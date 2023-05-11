
package org.scoja.server.action;

import org.scoja.server.core.Link;
import org.scoja.server.core.EventContext;

import org.scoja.server.source.Internal;
import org.scoja.common.PriorityUtils;

public class ActionLink extends Link {
    
    protected final Action action;
    
    public ActionLink(final Action action) {
        this.action = action;
    }
    
    public void process(final EventContext env) {
        action.exec(env);
        super.process(env);
    }
    
    public String toString() {
        return "Action " + action.toString();
    }
}
