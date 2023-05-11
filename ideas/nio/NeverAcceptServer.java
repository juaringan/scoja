import java.io.*;
import java.net.*;

public class NeverAcceptServer {

    public static void main(final String[] args)
    throws Exception {
        int i = 0;
        final int port = Integer.parseInt(args[i]);
        
        final ServerSocket ss = new ServerSocket(port);
        for (;;) { Thread.sleep(1); }
    }
}