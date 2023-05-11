
package org.scoja.server.filter;

import org.scoja.server.core.Linkable;
import org.scoja.server.core.LinkableAtPython;

public abstract class FilterLinkableAtPython
    extends LinkableAtPython
    implements Filter {

    public Linkable getLinkable() {
        return new FilteredLink(this);
    }
    
    public OrFilter __or__(final Filter other) {
        return new OrFilter(this, other);
    }
    
    public AndFilter __and__(final Filter other) {
        return new AndFilter(this, other);
    }
    
    public NotFilter __invert__() {
        return new NotFilter(this);
    }
}
