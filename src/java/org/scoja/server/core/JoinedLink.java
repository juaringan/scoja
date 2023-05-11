
package org.scoja.server.core;

public class JoinedLink
    extends FullLinkAtPython
    implements Linkable, DecoratedLink {

    protected final Linkable[] links;
    
    public JoinedLink(final Linkable l1, final Linkable l2) {
        this.links = new Linkable[] {l1, l2};
    }
    
    public JoinedLink(final Linkable[] links) {
        this.links = links;
    }
    
    public void addTarget(final Linkable target) {
        for (int i = 0; i < links.length; i++) {
            links[i].addTarget(target);
        }
    }

    public void addSimpleTarget(final Link target) {
        for (int i = 0; i < links.length; i++) {
            links[i].addSimpleTarget(target);
        }
    }

    public void addSimpleSource(final Link source) {
        for (int i = 0; i < links.length; i++) {
            links[i].addSimpleSource(source);
        }
    }
    
    public Linkable getLinkable() {
        return this;
    }
}
