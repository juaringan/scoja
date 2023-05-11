
import java.text.*;

public class FormatHoles {
    public static void main(final String[] args)
    throws Exception {
        final MessageFormat mf = new MessageFormat(args[0]);
        System.out.println("Holes of " + args[0]
                           + ": " + mf.getFormats().length);
        System.out.println("Formated: " + mf.format(args));
        System.out.println("Parsed length: " + mf.parse(args[0]).length);
    }
}
