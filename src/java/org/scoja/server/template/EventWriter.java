
package org.scoja.server.template;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.scoja.server.core.EventContext;

/**
 * Abstract the way to write a {@link org.scoja.server.core.Event} to
 * a target.
 */
public interface EventWriter {

    public String textFor(EventContext ectx);

    public void writeTo(PrintWriter out, EventContext ectx);
    
    public EventWriter asEventWriter();
    
    
    //======================================================================
    public static class Skeleton
        implements EventWriter {
        
        public String textFor(final EventContext context) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            writeTo(pw, context);
            pw.flush();
            return sw.toString();
        }
    
        public void writeTo(final PrintWriter out, final EventContext ectx) {
            out.print(textFor(ectx));
        }
        
        public EventWriter asEventWriter() {
            return this;
        }
    }

        
    //======================================================================
    /**
     * Implements the standard Syslog output format.
     * All {@link org.scoja.server.core.Event} subclases are supposed to
     * implement this standard format with method
     * {@link org.scoja.server.core.Event#writeTo(PrintWriter)}.
     */
    public static class Standard
        extends Skeleton {
        
        protected  static final Standard standard = new Standard();
        
        public static Standard getInstance() {
            return standard;
        }
        
        public void writeTo(final PrintWriter out, final EventContext evn) {
            evn.getEvent().writeTo(out);
        }
    }
}
