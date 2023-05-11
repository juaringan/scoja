// $Id: InterestBlockQ.java,v 1.1 2008/04/09 06:54:18 pedropalao-bk Exp $

import java.net.*;
import java.nio.channels.*;

/**
 * This code tests whether the implementation of
 * {@link SelectionKey#interestOps()}
 * blocks when a select operation is in progress.
 * <p>
 * It seems that none of the Sun JREs bocks on interestOps.
 */
public class InterestBlockQ {

    static Selector selector;

    public static void main(final String[] args)
    throws Exception {
        final ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.socket().bind(new InetSocketAddress(6969));
        selector = Selector.open();
        final SelectionKey key = ssc.register(selector, 0);
        new Thread(new Selection()).start();
        Thread.sleep(1000);
        System.out.println("Modifying interest");
        key.interestOps(SelectionKey.OP_ACCEPT);
        System.out.println("Interest modified");
        selector.wakeup();
    }
    
    public static class Selection implements Runnable {
        public void run() {
            try {
                do {
                    System.out.println("Going to select");
                    selector.select();
                } while (selector.selectedKeys().isEmpty());
            } catch (Throwable e) {
                e.printStackTrace(System.err);
            }
        }
    }
}
