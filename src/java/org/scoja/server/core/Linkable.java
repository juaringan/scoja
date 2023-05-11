
package org.scoja.server.core;

public interface Linkable {

    public void addTarget(final Linkable target);
    
    public void addSimpleTarget(final Link target);
    
    public void addSimpleSource(final Link source);
}
