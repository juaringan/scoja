
package org.scoja.server.filter;

import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;
import org.scoja.server.expr.StringExpression;

public class ContainsStringFilter extends FilterLinkableAtPython {

    protected final StringExpression expr;
    protected final String other;
    
    public ContainsStringFilter(final StringExpression expr,
                                final String other) {
        this.expr = expr;
        this.other = other;
    }

    public boolean isGood(final EventContext env) {
        final QStr value = expr.eval(env);
        return value.unqualified().indexOf(other) != -1;
    }
    
    public String toString() {
        return "testing whether " + expr + " contains " + other;
    }
}
