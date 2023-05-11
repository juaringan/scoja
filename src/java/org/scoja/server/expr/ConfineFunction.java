
package org.scoja.server.expr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.scoja.server.core.QStr;
import org.scoja.server.core.EventContext;

/**
 * A first try of a <tt>confine</tt> function
 * with history and progressive forgetting.
 * <p>
 * This functions remembers legal values during the last {@link #history}
 * milliseconds.
 * When called with a value <i>V</i>, several things can happend.
 * If <i>V</i> is a known legal value, the result is <i>V</i>.
 * If currently there is less than {@link #max} legal values,
 * <i>V</i> is remembered and returned.
 * Otherwise, {@link #confinedResult} is returned.
 * <p>
 * Current legal values are forgotten in chunks.
 * History is divided in {@link #periods} periods.
 * When a new period starts, the last one is dropped and all values
 * ocurring in it, and not ocurring in a more recent period, are forgotten.
 * <p>
 * This implementation tries to be as efficient as possible when the limit
 * is not reached.
 * For instance, it tries to do as few synchronizations as possible.
 * So, to add a value to a chunk, its set is copied so that other threads
 * don't get errors due to concurrent modifications.
 */
public class ConfineFunction extends String2StringFunction {

    private static final int USUAL_SIZE = 16;

    protected QStr confinedResult;
    protected int max;
    protected final long history;
    protected final int periods;
    protected final long periodExtend;

    protected final Object lock;    
    protected final Map/*<String,Integer>*/ inUse;
    protected final Cabin[] cabins;
    protected int currentCabin;
    
    public ConfineFunction(final StringExpression expr,
                           final String confinedResult, final int max, 
                           final long history, final int periods) {
        this(expr, QStr.checked(confinedResult), max, history, periods);
    }
    
    public ConfineFunction(final StringExpression expr,
                           final QStr confinedResult, final int max, 
                           final long history, final int periods) {
        super(expr);
        this.confinedResult = confinedResult;
        this.max = max;
        this.history = history;
        this.periods = periods;
        this.periodExtend = history / periods;
        
        this.lock = new Object();
        final long startTime = System.currentTimeMillis();
        this.inUse = new HashMap(getUsualSize());
        this.cabins = new Cabin[periods];
        for (int i = 0; i < periods; i++) {
            this.cabins[i] = new Cabin(startTime, getUsualSize());
        }
        this.currentCabin = 0;
    }
    
    public StringExpression cloneClean() {
        return cloneCleanWith(subexpr1.cloneClean());
    }
    
    protected StringExpression cloneCleanWith(final StringExpression subclon) {
        return new ConfineFunction(
            subclon, confinedResult, max, history, periods);
    }
    
    protected int getUsualSize() {
        return Math.min(USUAL_SIZE, max);
    }
    
    public QStr eval(final EventContext env) {
        final long now = System.currentTimeMillis();
        final QStr qarg1 = super.eval(env);
        final String value = QStr.unqualified(qarg1);
        boolean isLegal = true;
        
        Cabin c = cabins[currentCabin];
        if (!c.affects(now)) {
            synchronized (lock) {
                for (;;) {
                    c = cabins[currentCabin];
                    if (c.affects(now)) break;
                    final int nextCabin = (currentCabin+1) % periods;
                    cabins[nextCabin].dec(inUse);
                    cabins[nextCabin]
                        = new Cabin(c.getTimeLimit() + periodExtend,
                                    getUsualSize()); 
                    currentCabin = nextCabin;
                }
            }
        }
        if (!c.contains(value)) {
            synchronized (lock) {
                if (!c.contains(value)) {
                    final Integer times = (Integer)inUse.get(value);
                    if (times != null) {
                        c.add(value);
                        inUse.put(value, new Integer(times.intValue()+1));
                    } else if (inUse.size() < max) {
                        c.add(value);
                        inUse.put(value, new Integer(1));
                    } else {
                        isLegal = false;
                    }
                }
            }
        }

        final QStr result = isLegal ? qarg1 : confinedResult;
        //System.out.println("Confine " + qarg1 + " -> " + result);
        return result;
    }    
    
    private static final class Cabin {
        protected final long timeLimit;
        protected Set values;
        
        public Cabin(final long timeLimit, final int usualSize) {
            this.timeLimit = timeLimit;
            this.values = new HashSet(usualSize);
        }
        
        public long getTimeLimit() {
            return timeLimit;
        }
        
        public boolean affects(final long time) {
            return time < timeLimit;
        }
        
        public boolean contains(final String value) {
            return values.contains(value);
        }
        
        public void dec(final Map inUse) {
            for (Iterator it = values.iterator(); it.hasNext(); ) {
                final Object key = it.next();
                final Integer value = (Integer)inUse.get(key);
                if (value == null) {
                    // This should not happen
                } else if (value.intValue() == 1) {
                    inUse.remove(key);
                } else {
                    inUse.put(key, new Integer(value.intValue()-1));
                }
            }
        }
        
        public void add(final String value) {
            final Set newValues = new HashSet(values);
            newValues.add(value);
            values = newValues;
        }
    }
}
