
package org.scoja.server.filter;

import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;
import org.scoja.server.expr.StringExpression;

public class EqStringFilter extends FilterLinkableAtPython {

    protected final StringExpression expr;
    protected final String other;
    
    public EqStringFilter(final StringExpression expr, final String other) {
        this.expr = expr;
        this.other = other;
    }

    public boolean isGood(final EventContext env) {
        final QStr value = expr.eval(env);
        //Usually, neither value nor value.unqualified() are null,
        // but just in case:
        return (value == null || value.unqualified() == null) ? (other == null)
            : value.unqualified().equals(other);
    }
    
    public String toString() {
        return "testing whether " + expr + " equals " + other;
    }
}
