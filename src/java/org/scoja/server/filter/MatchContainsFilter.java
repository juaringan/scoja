
package org.scoja.server.filter;

import org.scoja.server.expr.StringExpression;
import org.scoja.server.core.EventContext;
import java.util.regex.*;

public class MatchContainsFilter extends MatchFilter {

    public MatchContainsFilter(final StringExpression expr,
                               final String regexp) {
        super(expr, regexp);
    }    
    
    protected String methodName() {
        return "part of";
    }
    
    protected boolean matchMethod(final Matcher matcher) {
        return matcher.find();
    }
}
