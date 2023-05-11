
package org.scoja.server.filter;

import org.scoja.server.core.EventContext;

public class AndFilter extends ConnectiveFilter {

    public AndFilter(final Filter[] filters) {
        super(filters);
    }
    
    public AndFilter(final Filter filter1, final Filter filter2) {
        this(new Filter[] {filter1, filter2});
    }

    public boolean isGood(final EventContext env) {
        for (int i = 0; i < filters.length; i++) {
            if (!filters[i].isGood(env)) return false;
        }
        return true;
    }
    
    public String operator() {
        return "&";
    }
}
