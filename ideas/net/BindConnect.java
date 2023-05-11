
import java.net.*;

/**
 * To test it is possible to connect to a socket that has been bind to
 * a fixed address.
 * <b>It is no possible.</b>
 * Binding is usually no purpose because the OS will assign an
 * arbitrary address when connecting; this address is usally enough.
 */
public class BindConnect extends Thread {

    public static void main(final String[] args) throws Exception {
        int argc = 0;
        final int port1 = Integer.parseInt(args[argc++]);
        final int port2 = Integer.parseInt(args[argc++]);
        new BindConnect(port1,port2).start();
        new BindConnect(port2,port1).start();
    }
    
    int toBind, toConnect;
    
    BindConnect(final int toBind, final int toConnect) {
        this.toBind = toBind;
        this.toConnect = toConnect;
    }
    
    public void run() {
        try {
            final InetAddress lh = InetAddress.getByName("localhost");
            final Socket s = new Socket();
            s.bind(new InetSocketAddress(lh,toBind));
            System.out.println("Bound at " + toBind);
            Thread.sleep(100);
            s.connect(new InetSocketAddress(lh,toConnect));
            System.out.println("Connected at " + toConnect);
            Thread.sleep(100);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
