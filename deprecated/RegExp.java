
package org.scoja.server.filter;

import org.scoja.server.core.EventContext;
import org.scoja.server.core.Environment;
import java.util.regex.*;

public class RegExp {

    protected final String expression;
    protected final Pattern pattern;
    protected final String[] names;

    public RegExp(final String expression) {
        this(expression, null);
    }
    
    public RegExp(final String expression, final String[] names) 
        throws PatternSyntaxException {
        this.expression = expression;
        this.pattern = Pattern.compile(expression);
        this.names = names;
    }
    
    public boolean match(final CharSequence str, final EventContext eenv) {
        final Matcher matcher = pattern.matcher(str);
        final boolean matches = matcher.matches();
        if (!matches) return false;
        if (names != null) {
            final Environment env = eenv.getEnvironment();
            for (int i = 0; i < names.length; i++) {
                if (names[i] != null) {
                    env.define(names[i], matcher.group(i+1));
                }
            }
        }
        return true;
    }
}
