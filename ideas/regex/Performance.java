
import java.util.regex.*;

public class Performance {

    public static void main(final String[] args) {
        if (args.length < 2) {
            System.err.println(
                "java " + Performance.class + " <n> <regex> <values...>");
            System.exit(1);
        }
        
        int i = 0;
        final int n = Integer.parseInt(args[i++]);
        final String regex = args[i++];
        final String[] data = new String[args.length-i];
        System.arraycopy(args,i, data,0,data.length);
     
        final Pattern pat = Pattern.compile(regex);
        test(pat, Math.min(n,10000), data);
        test(pat, n, data);
    }
    
    static void test(final Pattern pat, final int n, final String[] data) {
        final long t0 = System.nanoTime();
        int matches = 0, groups = 0, nnGroups = 0;
        for (int i = 0; i < n; i++) {
            final Matcher m = pat.matcher(data[i%data.length]);
            if (m.lookingAt()) {
                matches++;
                groups += m.groupCount();
                nnGroups += nonNullGroups(m);
            }
        }
        System.err.println(n + "->" + matches + "/" + groups + "/" + nnGroups
                + ": " + (System.nanoTime() - t0)/1e9);
    }
    
    static int nonNullGroups(final Matcher m) {
        final int n = m.groupCount();
        int c = 0;
        for (int i = 1; i <= n; i++) if (m.group(i) != null) c++;
        return c;
    }
}
