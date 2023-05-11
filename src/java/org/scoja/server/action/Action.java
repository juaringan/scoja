
package org.scoja.server.action;

import org.scoja.server.core.EventContext;

public interface Action {

    public void exec(final EventContext env);
}
