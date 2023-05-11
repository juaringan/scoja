
package org.scoja.server.action;

import org.scoja.common.PriorityUtils;
import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;
import org.scoja.server.source.Internal;

public class EnvironmentSetString extends ActionLinkableAtPython {

    protected final String var;
    protected final QStr value;

    public EnvironmentSetString(final String var, final String value) {
        this.var = var;
        this.value = QStr.checked(value);
    }

    public void exec(final EventContext env) {
        env.getEnvironment().define(var, value);
        if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
            Internal.debug(env, Internal.ACTION, "Set a (new) value for \""
                           + var + "\" at " + env);
        }
    }
    
    public String toString() {
        return "to set var \"" +var+ "\" to value \"" +value+ "\"";
    }
}
