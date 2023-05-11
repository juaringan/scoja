
package org.scoja.server.core;

public class ChainedLink 
    extends FullLinkAtPython
    implements Linkable, DecoratedLink {

    protected final Linkable[] links;
    
    public ChainedLink(final Linkable l1, final Linkable l2) {
        this.links = new Linkable[] {l1, l2};
    }
    
    public ChainedLink(final Linkable[] links) {
        this.links = links;
    }
    
    public void addTarget(final Linkable target) {
        links[links.length-1].addTarget(target);
    }

    public void addSimpleTarget(final Link target) {
        links[links.length-1].addSimpleTarget(target);
    }

    public void addSimpleSource(final Link source) {
        links[0].addSimpleSource(source);
    }
    
    public Linkable getLinkable() {
        return this;
    }
}
