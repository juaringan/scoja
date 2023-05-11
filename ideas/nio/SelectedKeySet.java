// $Id: SelectedKeySet.java,v 1.1 2003/05/13 10:20:14 elmartinfierro Exp $

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

public class SelectedKeySet {
    public static void main(String[] args) throws Exception {
	int i = 0;
	final int port = Integer.parseInt(args[i++]);
	
	final Selector selector = Selector.open();
	final ServerSocketChannel server = ServerSocketChannel.open();
	server.configureBlocking(false);
	server.socket().bind(new InetSocketAddress(port));
	server.register(selector, SelectionKey.OP_ACCEPT);
	
        System.out.println("Init: " + selector.selectedKeys());
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        System.out.print("Waiting first connection...");
        selector.select();
        Set s = selector.selectedKeys();
        System.out.println("received: " + s + " (" + s.hashCode() + ")");
        Iterator it = s.iterator();
        final SelectionKey sk = (SelectionKey)it.next();
        sk.interestOps(0);
        it.remove();
        System.out.print("Waiting second connection...");
        new Thread() {
                public void run() {
                    try {
                        Thread.sleep(3000);
                        sk.interestOps(SelectionKey.OP_ACCEPT);
                        sk.selector().wakeup();
                    } catch (Exception e) {}
                }
            }.start();
        selector.select();
        s = selector.selectedKeys();
        System.out.println("received: " + s + " (" + s.hashCode() + ")");
        System.out.print("Waiting third connection...");
        selector.select();
        s = selector.selectedKeys();
        System.out.println("received: " + s + " (" + s.hashCode() + ")");
    }
}
