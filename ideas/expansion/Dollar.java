
import java.util.regex.*;

public class Dollar {

    public static void main(final String[] args) throws Exception {
        int argc = 0;
        final String expr = args[argc++];
        final Pattern pattern = Pattern.compile(expr);
        while (argc < args.length) {
            final String data = args[argc++];
            System.out.println("Matching " + data);
            final Matcher matcher = pattern.matcher(data);
            while (matcher.find()) {
                System.out.println("  a match found");
                for (int i = 0; i <= matcher.groupCount(); i++) {
                    System.out.println("    group " + i
                                       + " from " + matcher.start(i)
                                       + " to " + matcher.end(i)
                                       + " is " + matcher.group(i));
                }
            }
        }
    }
}
