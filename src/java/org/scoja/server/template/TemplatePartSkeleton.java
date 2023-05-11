
package org.scoja.server.template;

import java.io.PrintWriter;

import org.scoja.server.core.EventContext;

public abstract class TemplatePartSkeleton 
    extends EventWriter.Skeleton
    implements TemplatePart {

    public void toString(final StringBuffer sb) {
        sb.append(toString());
    }
    
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        toString(sb);
        return sb.toString();
    }
}
