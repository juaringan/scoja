
package org.scoja.server.filter;

import org.scoja.server.expr.StringExpression;
import org.scoja.server.core.EventContext;
import java.util.regex.*;

public class MatchBeginningFilter extends MatchFilter {

    public MatchBeginningFilter(final StringExpression expr,
                                final String regexp) {
        super(expr, regexp);
    }    
    
    protected String methodName() {
        return "beginning of";
    }
    
     protected boolean matchMethod(final Matcher matcher) {
        return matcher.lookingAt();
    }
}
