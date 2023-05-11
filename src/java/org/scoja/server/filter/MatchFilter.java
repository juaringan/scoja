
package org.scoja.server.filter;

import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;
import org.scoja.server.core.Environment;
import org.scoja.server.expr.StringExpression;
import java.util.regex.*;

public abstract class MatchFilter extends FilterLinkableAtPython {

    protected final StringExpression expr;
    protected final String regexp;
    protected final Pattern pattern;
    protected QStr unknown;
    protected int[] groupsToExtract;
    protected String[] varsToDefine;
    protected int[] qualities;
    protected String varForTail;

    public MatchFilter(final StringExpression expr, final String regexp) {
        this.expr = expr;
        this.regexp = regexp;
        this.pattern = Pattern.compile(regexp);
        this.unknown = Environment.Q_UNKNOWN;
        this.groupsToExtract = null;
        this.varsToDefine = null;
        this.qualities = null;
        this.varForTail = null;
    }
    
    public void setUnknown(final QStr unknown) {
        this.unknown = unknown;
    }
    
    public void setUnknown(final String unknown) {
        this.unknown = QStr.checked(unknown);
    }
    
    public void setVarsToDefine(final int[] groupsToExtract,
                                final String[] varsToDefine,
                                final int[] qualities) {
        this.groupsToExtract = groupsToExtract;
        this.varsToDefine = varsToDefine;
        this.qualities = qualities;
    }
    
    public void setVarToDefineWithTail(final String varForTail) {
        this.varForTail = varForTail;
    }
    
    public boolean isGood(final EventContext ectx) {
        return match(expr.eval(ectx).unqualified(), ectx);
    }
        
    protected abstract String methodName();
    protected abstract boolean matchMethod(final Matcher matcher);
    
    protected boolean match(final String val,
            final EventContext ectx) {
        final Matcher matcher = pattern.matcher(val);
        final boolean matches = matchMethod(matcher);
        if (matches) defineVars(val, matcher, ectx);
        return matches;
    }
    
    protected void defineVars(final String val, final Matcher matcher, 
            final EventContext ectx) {
        final Environment env = ectx.getEnvironment();
        if (varsToDefine != null) {
            for (int i = 0; i < varsToDefine.length; i++) {
                final String cap = matcher.group(groupsToExtract[i]);
                final QStr qcap = (cap == null) 
                    ? unknown
                    : new QStr(cap, qualities[i]);
                env.define(varsToDefine[i], qcap);
            }
        }
        if (varForTail != null) {
            env.define(varForTail,val.substring(matcher.end()));
        }
    }
    
    public String toString() {
        return "testing whether " + methodName() + " " + expr
            + " match " + regexp;
    }
}
