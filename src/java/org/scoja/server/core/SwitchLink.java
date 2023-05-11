
package org.scoja.server.core;

import java.util.Map;
import java.util.HashMap;

import org.scoja.server.expr.StringExpression;


public class SwitchLink extends Link {

    protected final StringExpression expr;
    protected final Map targetMap;
    protected Link defaultTarget;

    public SwitchLink(final StringExpression expr,
                      final String[] keys,
                      final DecoratedLink[] keyedTargets) {
        super();
        this.expr = expr;
        this.defaultTarget = null;
        if (keys.length != keyedTargets.length) {
            throw new IllegalArgumentException
                ("Number of keys (" +keys.length+ ") differs from"
                 + " number of keyed targets (" +keyedTargets.length+ ")");
        }
        this.targetMap = new HashMap(keys.length);
        for (int i = 0; i < keys.length; i++) {
            this.targetMap.put(keys[i], buildLink(keyedTargets[i]));
        }
    }
    
    public void setDefault(final DecoratedLink target) {
        this.defaultTarget = buildLink(target);
    }
    
    private Link buildLink(final DecoratedLink target) {
        Link finalTarget;
        if (target instanceof Link) {
            finalTarget = (Link)target;
        } else {
            finalTarget = new Link();
            finalTarget.addTarget(target.getLinkable());
        }
        return finalTarget;
    }
    
    public void propagate(final EventContext ectx) {
        final String key = expr.eval(ectx).unqualified();
        final Link target = (Link)targetMap.get(key);
        if (target != null) {
            target.process(ectx);
        } else if (defaultTarget != null) {
            defaultTarget.process(ectx);
        }
        super.propagate(ectx);
    }
}
