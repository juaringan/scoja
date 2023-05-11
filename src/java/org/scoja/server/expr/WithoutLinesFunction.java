
package org.scoja.server.expr;

import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;

/**
 * Remove <i>end of lines</i> from an String.
 * Both CR and LF are considered line enders.
 * White space (space, CR, LF, tabs, ..) after an end of line is removed.
 * <p>
 * At configuration files, it is mapped to <code>withoutEOLN</code>
 * function.
 */
public class WithoutLinesFunction extends String2StringFunction {
    
    public WithoutLinesFunction(final StringExpression subexpr) {
        super(subexpr);
    }
    
    protected StringExpression cloneCleanWith(final StringExpression subclon) {
        return new WithoutLinesFunction(subclon);
    }
    
    public QStr eval(final EventContext env) {
        final QStr qarg1 = super.eval(env);
        if (!qarg1.hasEOLN()) return qarg1;
        
        final String arg1 = qarg1.unqualified();
        final int len = arg1.length();
        final char[] buffer = new char[len];
        int s = 0, t = 0;
        
        boolean wasSpace = true, wasEOLN = true;
        while (s < len) {
            final char c = arg1.charAt(s++);
            if (c == '\n' || c == '\r') {
                if (!wasSpace) {
                    buffer[t++] = ' ';
                    wasSpace = true;
                }
                wasEOLN = true;
            } else if (wasEOLN && c <= ' ') {
            } else {
                buffer[t++] = c;
                wasSpace = c <= ' ';
                wasEOLN = wasSpace && wasEOLN;
            }
        }
        
        final int qualities
            = (qarg1.qualities() & ~QStr.HASNT_EOLN) | QStr.HAS_EOLN;
        return new QStr(new String(buffer, 0, t), qualities);
    }
}
