
import java.util.*;
        
public class EnvThread extends Thread {

    protected final Map env;

    public EnvThread(final Runnable torun) {
        super(torun);
        this.env = new HashMap();
    }
    
    public void put(final Object key, final Object value) {
        env.put(key, value);
    }
    
    public Object get(final Object key) {
        return env.get(key);
    }
}
