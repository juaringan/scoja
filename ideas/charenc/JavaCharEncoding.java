
import java.nio.charset.*;
import java.util.*;

/**
 * There are several solutions to the problem of passing a String to C
 * code through JNI:
 * (1) passing an String object and calling GetStringUTFChars
 * (2) passing an String object and calling its method getBytes()
 * (3) passing an array of bytes, each one representing a C char.
 * <p>
 * The first one is totally flawed, although it seems to be the one
 * recommended in all JNI documents.
 * Function GetStringUTFChars can only be used if the underlying system
 * is using UTF-8 encoding.
 * <p>
 * So, conversion should be done knowing the system encoding;
 * String.getBytes() do that, as can be tested with this program.
 * If LANG environment variable is set to en_US.ISO-8859-1, each Java char
 * will be transformed into one byte;
 * when set to en_US.UTF-8, the corresponding utf-8 encoding will be performed.
 * <p> 
 * The second and third ones are equivalent if the byte array is computed
 * with getBytes() at Java, but the third one is by far the simplest one
 * because calling a Java method from C is at least embarrasing.
 */
public class JavaCharEncoding {

    private static final String[] encodings = {
        "ISO-8859-1",
        "UTF-8",
        "UTF-16",
    };

    public static void main(final String[] args)
    throws Exception {
        System.out.println("Available char encodings");
        for (Iterator it = Charset.availableCharsets().keySet().iterator();
             it.hasNext(); ) {
            System.out.println("  " + it.next());
        }
        
        for (int i = 0; i < args.length; i++) {
            System.out.println("Argument " + i);
            final byte[] system = args[i].getBytes();
            System.out.println("  size of system encoding: " + system.length);
            for (int j = 0; j < encodings.length; j++) {
                final byte[] bs = args[i].getBytes(encodings[j]);
                System.out.println("  size of " + encodings[j]
                                   + ": " + bs.length);
            }
        }
    }
}
