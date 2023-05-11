
package org.scoja.server.action;

import org.scoja.common.PriorityUtils;
import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;
import org.scoja.server.expr.StringExpression;
import org.scoja.server.source.Internal;

public class EnvironmentSetExpression extends ActionLinkableAtPython {

    protected final String var;
    protected final StringExpression expr;

    public EnvironmentSetExpression(final String var,
                                    final StringExpression expr) {
        this.var = var;
        this.expr = expr;
    }

    public void exec(final EventContext env) {
        final QStr value = expr.eval(env);
        env.getEnvironment().define(var, value);
        if (PriorityUtils.DEBUG <= Internal.LOG_DETAIL) {
            Internal.debug(env, Internal.ACTION, "Set a (new) value for \""
                           + var + "\" at " + env);
        }
    }
    
    public String toString() {
        return "to set var \"" +var+ "\" to the evaluation of " + expr;
    }
}
