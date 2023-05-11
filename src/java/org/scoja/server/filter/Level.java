
package org.scoja.server.filter;

import org.scoja.server.core.EventContext;

public class Level extends IntSetFilter {

    public Level(final int level) {
        super(level);
    }
    
    public Level(final int[] levels) {
        super(levels);
    }

    public boolean isGood(final EventContext env) {
        return isGood(env.getEvent().getLevel());
    }

    public String getConceptName() {
        return "levell";
    }
}
