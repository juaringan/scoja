
import java.util.*;

public abstract class Ellapsed {

    public static void main(final String[] args) throws Exception {
        int argc = 0;
        final String method = args[argc++];
        final int heater = Integer.parseInt(args[argc++]);
        final int total = Integer.parseInt(args[argc++]);
        final double prob = Double.parseDouble(args[argc++]);
        
        final Class methodClass = Class.forName(method);
        final Ellapsed et = (Ellapsed)methodClass.newInstance();
        et.init(prob);
        System.out.println("Heating JNI");
        et.execute(heater);
        System.out.println("Performance meter");
        final long t1 = System.currentTimeMillis();
        final int captured = et.execute(total);
        final long t2 = System.currentTimeMillis();
        System.out.println("Ellapsed: " + (t2-t1)/1000.0);
        System.out.println(captured + " exceptions");
    }
    
    protected double probability;
    protected Random random;
    
    public void init(final double probability) {
        this.probability = probability;
        this.random = new Random();
    }

    public int execute(final int total) {
        int excp = 0;
        for (int i = 0; i < total; i++) {
            try {
                final Object x = randomError();
                if (x == null) excp++;
            } catch (Exception e) {
                excp++;
            }
        }
        return excp;
    }
    
    public abstract Object randomError() throws Exception;
}
