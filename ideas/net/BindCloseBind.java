import java.net.*;

/**
 * To test how much a close() invalidates a Socket.
 * A closed socket cannot be used any more:
 * for instance, it cannot be rebound.
 */
public class BindCloseBind {

    public static void main(final String[] args) throws Exception {
        final InetSocketAddress addrs
            = new InetSocketAddress("localhost", 3333);
        final ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(addrs);
        System.err.println("Bound: " + ss.isBound()
                + ", closed: " + ss.isClosed()
                + ", local: " + ss.getLocalSocketAddress());
        ss.close();
        System.err.println("Bound: " + ss.isBound()
                + ", closed: " + ss.isClosed()
                + ", local: " + ss.getLocalSocketAddress());
        ss.bind(addrs);
        System.err.println("Bound: " + ss.isBound()
                + ", local: " + ss.getLocalSocketAddress());
    }
}
