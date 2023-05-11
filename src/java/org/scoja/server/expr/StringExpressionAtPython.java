
package org.scoja.server.expr;

import org.scoja.server.core.EventContext;
import org.scoja.server.filter.Filter;
import org.scoja.server.filter.EqStringFilter;

public abstract class StringExpressionAtPython implements StringExpression {

    public Filter __eq__(final String other) {
        return new EqStringFilter(this, other);
    }
    
    public StringExpression cloneClean() { return this; }
}
