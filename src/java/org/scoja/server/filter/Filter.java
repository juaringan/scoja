
package org.scoja.server.filter;

import org.scoja.server.core.EventContext;

public interface Filter {

    public boolean isGood(final EventContext env);
}
