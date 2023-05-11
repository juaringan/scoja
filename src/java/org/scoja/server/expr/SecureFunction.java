
package org.scoja.server.expr;

import java.io.File;

import org.scoja.cc.text.escaping.Escaper;
import org.scoja.cc.text.escaping.Collapsing;

import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;

/**
 * Makes a String secure to be used as part of a file name.
 * Put an underline (<code>_</code>) instead of end of lines,
 * {@link File#separatorChar} or {@link File#pathSeparatorChar}.
 */
public class SecureFunction extends String2StringFunction {
    
    //======================================================================
    private static Escaper securer = Collapsing.secureForWhatever();
    
    public static void setAlgorithm(final Escaper securer) {
        SecureFunction.securer = securer;
    }
    
    public static Escaper getAlgorithm() {
        return securer;
    }
    
    public static String secure(final String unsecure) {
        //System.err.println("SECURITING: " + unsecure);
        return securer.escaped(unsecure);
    }
    

    //======================================================================
    public SecureFunction(final StringExpression subexpr) {
        super(subexpr);
    }
    
    protected StringExpression cloneCleanWith(final StringExpression subclon) {
        return new SecureFunction(subclon);
    }
    
    public QStr eval(final EventContext env) {
        final QStr qarg1 = super.eval(env);
        if (qarg1.isFilenameSecure()) return qarg1;
        
        final String secured = secure(qarg1.unqualified());
        final int qualities
            = (qarg1.qualities() 
               & ~QStr.ISNT_FILENAME_SECURE
               & ~QStr.HAS_EOLN) 
            | QStr.IS_FILENAME_SECURE
            | QStr.HASNT_EOLN;
        return new QStr(secured, qualities);
    }
    
}
