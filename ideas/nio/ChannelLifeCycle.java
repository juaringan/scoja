import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * To explore a SocketChannel lifecycle.
 *
 * <p>
 * Several tests are equivalent:
 *   channel.isOpen() == !channel.socket().isClosed()
 *   channel.isConnected() == channel.socket().isConnected()
 * When connecting,
 *   channel.isConnected() == channel.socket().isBound()
 *
 * <p>
 * When in non-blocking mode, isInputShutdown is not refreshed.
 * But at the end of input, the channel becames always selectable for reading.
 *
 * <p>
 * A SocketChannel can be in 4 states: open, pending, connected, closed.
 * It came to life open.
 *   * --[close()]--> close
 *   open --[connect(..)]--> pending
 *   pending --[finishConnect() without error]--> connected
 *   pending --[finishConnect() with exception]--> closed
 *
 * <p>
 * A channel cannot be used for reading or writing until it is connected.
 * In any other state, a read/write operation raises an exception:
 * NotYetConnectedException, ClosedChannelException.
 */
public class ChannelLifeCycle {

    public static void main(final String[] args)
    throws Exception {
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        final InetSocketAddress addrs = new InetSocketAddress(host, port);
        int n;
        SocketChannel socket = SocketChannel.open();
        showStatus("After open", socket);
        socket.close();
        showStatus("After close", socket);
        //Cannot close after a close:
        //  java.nio.channels.ClosedChannelException
        //socket.connect(addrs);
        
        socket = SocketChannel.open();
        showStatus("After open", socket);
        socket.configureBlocking(false);
        socket.connect(addrs);
        showStatus("After connect", socket);
        final ByteBuffer request = ByteBuffer.wrap(
            "GET / HTTP/1.0\r\n\r\n".getBytes());
        //int n = socket.write(request);
        //showStatus("After write " + n, socket);
        try {
            while (!socket.isConnected()) socket.finishConnect();
        } catch (Throwable ignored) {}
        showStatus("After connection", socket);
        if (!socket.isOpen()) return;
        
        n = socket.write(request);
        showStatus("After write " + n, socket);
        socket.socket().shutdownOutput();
        final ByteBuffer bs = ByteBuffer.allocate(16*1024);
        final Selector sel = Selector.open();
        socket.register(sel, SelectionKey.OP_READ, null);
        int eos = 0;
        for (;;) {
            bs.clear();
            sel.select();
            showStatus("After selection " + sel.selectedKeys(), socket);
            n = socket.read(bs);
            showStatus("After reading " + n, socket);
            if (n == -1) {
                eos++;
                if (eos > 3) break;
            }
        }
        socket.close();
        showStatus("After close", socket);
    }
    
    protected static void showStatus(final String title,
            final SocketChannel socket) {
        System.err.println(
            title
            + "\n  open: " + socket.isOpen()
            + ", connection pending: " + socket.isConnectionPending()
            + ", connected: " + socket.isConnected()
            + "\n  bound: " + socket.socket().isBound()
            + ", closed: " + socket.socket().isClosed()
            + ", connected: " + socket.socket().isConnected()
            + ", in shutdown: " + socket.socket().isInputShutdown()
            + ", out shutdown: " + socket.socket().isOutputShutdown()
            );
    }
}
