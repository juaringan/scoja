
package org.scoja.server.core;

import java.io.File;

import org.scoja.common.PriorityUtils;
import org.scoja.server.expr.SecureFunction;
import org.scoja.server.source.Internal;

/**
 * A Qualified String is an String which carries several properties,
 * usually related to whether it is secure to use it to form a kind of value.
 * For instance, "whether it is secure to use the String as a
 * filename" o "whether the String contains end of lines".
 */
public final class QStr {

    public static final int NO_EXPLICIT_QUALITY = 0;
    public static final int IS_FILENAME_SECURE = 1;
    public static final int ISNT_FILENAME_SECURE = 2;
    public static final int FILENAME_MASK
        = IS_FILENAME_SECURE | ISNT_FILENAME_SECURE;
    public static final int HASNT_EOLN = 4;
    public static final int HAS_EOLN = 8;
    public static final int EOLN_MASK
        = HASNT_EOLN | HAS_EOLN;
    
    public static final int PERFECT
        = IS_FILENAME_SECURE
        | HASNT_EOLN;

    
    protected static final String[] QUALITY_NAMES = {
        "is filename secure",
        "isn't filename secure",
        "has EOLN",
        "hasn't EOLN"
    };
    
    /**
     * Check whether it is secure to access to a file with a name
     * containing <code>str</code>.
     * Uses {@link Collapsing#forWhatever()}.
     */
    public static boolean isFilenameSecure(final String str) {
        return !SecureFunction.getAlgorithm().isAffected(str);
    }
    
    /**
     * Check whether <code>str</code> contains an End of Line.
     * Both '\n' and '\r' are considered end of line characters.
     */
    public static boolean hasEOLN(final String str) {
        final int len = str.length();
        for (int i = 0; i < len; i++) {
            final char c = str.charAt(i);
            if (c == '\n' || c == '\r') return true;
        }
        return false;
    }

        
    //======================================================================

    protected final String str;
    protected int qualities;
    
    public QStr(final String str) {
        this(str, 0);
    }
    
    public QStr(final String str, final int qualities) {
        this.str = str;
        this.qualities = qualities;
    }
    
    public static QStr supposePerfect(final String str) {
        return new QStr(str, PERFECT);
    }
    
    public static QStr checked(final String str) {
        final QStr qstr = new QStr(str);
        qstr.checkIsFilenameSecure();
        qstr.checkHasEOLN();
        return qstr;
    }
    
    public static QStr unchecked(final String str) {
        return new QStr(str);
    }
    
    public String unqualified() {
        return str;
    }
    
    public static String unqualified(final QStr qs) {
        return (qs == null) ? null : qs.unqualified();
    }
    
    public int qualities() {
        return qualities;
    }
    
    public boolean isKnownThat(final int mask) {
        return (qualities & mask) == mask;
    }
    
    public void rememberThat(final int mask) {
        qualities |= mask;
    }
    
    public boolean isFilenameSecure() {
        int bits = qualities & FILENAME_MASK;
        if (bits == 0) bits = checkIsFilenameSecure();
        return bits == IS_FILENAME_SECURE;
    }
    
    protected int checkIsFilenameSecure() {
        final int mask = isFilenameSecure(str)
            ? IS_FILENAME_SECURE : ISNT_FILENAME_SECURE;
        qualities |= mask;
        return mask;
    }

    public boolean hasEOLN() {
        int bits = qualities & EOLN_MASK;
        if (bits == 0) bits = checkHasEOLN();
        return bits == HASNT_EOLN;
    }
    
    protected int checkHasEOLN() {
        final int mask = hasEOLN(str) ? HAS_EOLN : HASNT_EOLN;
        qualities |= mask;
        return mask;
    }
    
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(str).append("![");
        boolean first = true;
        for (int i = 0; i < QUALITY_NAMES.length; i++) {
            if ((qualities & (1 << i)) != 0) {
                if (first) first = false;
                else sb.append(", ");
                sb.append(QUALITY_NAMES[i]);
            }
        }
        sb.append(']');
        return sb.toString();
    }
}
