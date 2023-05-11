
package org.scoja.server.expr;

import java.util.regex.*;

import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;

/**
 */
public class RegexMappingFunction extends String2StringFunction {

    protected final String[] keys;
    protected final Pattern[] keyPatterns;
    protected final String[] values;
    protected final int[] qualities;
    protected QStr defaultResult;
    
    public RegexMappingFunction(final StringExpression subexpr,
            final String[] keys,
            final String[] values,
            final int[] qualities) 
        throws PatternSyntaxException {
        super(subexpr);
        this.defaultResult = null;
        if (keys.length != values.length || keys.length != qualities.length) {
            throw new IllegalArgumentException
                ("Number of keys (" +keys.length+ ") differs"
                 + " from number of values (" +values.length+ ")"
                 + " or from number of qualities (" +qualities.length+ ")");
        }
        this.keys = keys;
        this.values = values;
        this.qualities = qualities;
        this.keyPatterns = new Pattern[keys.length];
        for (int i = 0; i < keys.length; i++) {
            this.keyPatterns[i] = Pattern.compile(keys[i]);
        }
    }
    
    public void setDefault(final String defaultResult) {
        this.defaultResult = QStr.checked(defaultResult);
    }
    
    protected RegexMappingFunction(final StringExpression subexpr,
            final RegexMappingFunction other) {
        super(subexpr);
        this.keys = other.keys;
        this.keyPatterns = other.keyPatterns;
        this.values = other.values;
        this.qualities = other.qualities;
        this.defaultResult = other.defaultResult;
    }
    
    protected StringExpression cloneCleanWith(final StringExpression subclon) {
        return new RegexMappingFunction(subclon, this);
    }
    
    public QStr eval(final EventContext env) {
        final QStr qarg1 = super.eval(env);
        final String arg1 = qarg1.unqualified();
        
        for (int i = 0; i < keyPatterns.length; i++) {
            final Matcher matcher = keyPatterns[i].matcher(arg1);
            if (matcher.find()) {
                final StringBuffer sb = new StringBuffer();
                matcher.appendReplacement(sb, values[i]);
                matcher.appendTail(sb);
                return new QStr(sb.toString(), qualities[i]);
            }
        }
        if (defaultResult != null) return defaultResult;
        else return qarg1;
    }
}
