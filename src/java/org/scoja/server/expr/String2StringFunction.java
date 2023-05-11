
package org.scoja.server.expr;

import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;

public abstract class String2StringFunction extends StringExpressionAtPython {
    
    protected final StringExpression subexpr1;
    
    public String2StringFunction(final StringExpression subexpr1) {
        this.subexpr1 = subexpr1;
    }
    
    public QStr eval(final EventContext env) {
        return subexpr1.eval(env);
    }    
    
    public StringExpression cloneClean() {
        final StringExpression subclon = subexpr1.cloneClean();
        return (subclon == subexpr1) ? this : cloneCleanWith(subclon);
    }
    
    protected abstract StringExpression cloneCleanWith(
        StringExpression subclon);
}
