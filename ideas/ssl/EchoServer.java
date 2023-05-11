
import java.net.*;
import javax.net.ssl.*;

/**
 * A simple SSL server for testing purposes.
 * It echoes every character it receives.
 */
public class EchoServer implements Runnable {
    public static void main(final String[] args) throws Exception {
        if (args.length == 0) {
            System.err.print(
                "java -Djavax.net.ssl.keyStore=server.jks"
                + " -Djavax.net.ssl.keyStorePassword=culo69"
                + " EchoServer <ip> <port>"
                + "\n");
            System.exit(1);
        }
        final String ip = args[0];
        final int port = Integer.parseInt(args[1]);
        
        final SSLServerSocket ss = (SSLServerSocket)
            SSLServerSocketFactory.getDefault().createServerSocket();
        ss.bind(new InetSocketAddress(ip, port));
        ss.setWantClientAuth(false);
        for (;;) {
            final Socket s = ss.accept();
            new Thread(new EchoServer(s)).start();
        }
    }
    
    final Socket socket;
    
    public EchoServer(final Socket socket) {
        this.socket = socket;
    }
    
    public void run() {
        final byte[] buffer = new byte[64*1024];
        try {
            for (;;) {
                final int n = socket.getInputStream().read(buffer);
                System.err.println("Read " + n);
                if (n < 0) break;
                socket.getOutputStream().write(buffer, 0, n);
                socket.getOutputStream().flush();
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }
}
