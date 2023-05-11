
package org.scoja.server.filter;

import org.scoja.server.expr.StringExpression;
import org.scoja.server.core.EventContext;
import java.util.regex.*;

public class MatchAllFilter extends MatchFilter {

    public MatchAllFilter(final StringExpression expr, final String regexp) {
        super(expr, regexp);
    }    
    
    protected String methodName() {
        return "all";
    }
    
    protected boolean matchMethod(final Matcher matcher) {
        return matcher.matches();
    }
}
