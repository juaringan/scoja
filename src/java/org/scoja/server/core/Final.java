
package org.scoja.server.core;

public class Final extends Link {
    
    public void process(final EventContext env) {
        env.complete();
    }    
}
