
package org.scoja.server.core;

import java.util.Map;

/**
 * This is a map from variables (String) to String values.
 */
public interface Environment {

    public static final String UNKNOWN = "UNKNOWN";
    public static final QStr Q_UNKNOWN = QStr.checked(UNKNOWN);

    /**
     * Put a mark. All modificatons (variables defined or redefined)
     * after executing this operation will be forgotten after
     * executing {@link #release()}.
     */
    public void mark();
    
    /**
     * Make this enviroment to forget al modification after the last
     * execution of {@link #mark()}.
     */
    public void release();

    public boolean isDefined(final String var);

    /**
     * Set a (new) <code>value</code> for variable <code>var</code>
     */
    public void define(final String var, final String value); 
    
    /**
     * Set a (new) <code>value</code> for variable <code>var</code>
     */
    public void define(final String var, final QStr value);
    
    /**
     * Return the value give to <code>var</code>; <code>null</code> if
     * no <code>var</code> is undefined.
     */
    public QStr definition(final String var);
    
    /**
     * Set the preferred <i>character sequence</i> to mark an unknown
     * value.
     */
    public void unknown(String value);
    
    /**
     * Set the preferred <i>character sequence</i> to mark an unknown
     * value.
     */
    public void unknown(QStr value);
    
    /**
     * Return the preferred <i>character sequence</i> to mark an unknown
     * value. Usually, it is just {@link #Q_UNKNOWN}.
     */
    public QStr unknown();
}
