
package org.scoja.server.filter;

import org.scoja.server.core.EventContext;

public abstract class ConnectiveFilter extends FilterLinkableAtPython {

    protected final Filter[] filters;
    
    public ConnectiveFilter(final Filter[] filters) {
        this.filters = filters;
    }
    
    public abstract String operator();
    
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < filters.length; i++) {
            if (i > 0) sb.append(' ').append(operator()).append(' ');
            sb.append('(').append(filters[i]).append(')');
        }
        return sb.toString();
    }
}
