
package org.scoja.server.action;

import org.scoja.server.core.EventContext;
import org.scoja.server.source.Internal;

public class UseSendTimestamp extends ActionLinkableAtPython {

    protected final boolean useSend;

    public UseSendTimestamp(final boolean useSend) {
        this.useSend = useSend;
    }

    public void exec(final EventContext env) {
        env.getEvent().chooseReceptionAsPreferredTimestamp(!useSend);
    }
    
    public String toString() {
        return "to select "
            + (useSend ? "sended" : "receiving") + " timestamp";
    }
}
