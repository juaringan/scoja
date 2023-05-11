
import java.io.*;

/**
 * A test to see Java behaviour with a fifo.
 * If you run this program with /proc/kmsg, you will see how Unix cheats
 * Java: characters will appear at the console whenever the kernel writes to
 * /proc/kmsg.
 * So fifo nodes can be processed with a normal FileInputStream.
 */
public class ReadFifo {
    public static void main(final String[] args) throws Exception {
        final InputStream is = new FileInputStream(args[0]);
        for (;;) {
            final int c = is.read();
            if (c == -1) break;
            System.out.write(c);
        }
    }
}
