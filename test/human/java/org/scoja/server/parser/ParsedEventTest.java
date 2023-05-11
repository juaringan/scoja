
package org.scoja.server.parser;

import java.net.InetAddress;
import org.scoja.trans.RemoteInfo;
import org.scoja.server.core.Event;

public class ParsedEventTest {

    public static void main(final String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            final byte[] pack = args[i].getBytes();
            final Event event = new ParsedEvent(
                new RemoteInfo.Inet(InetAddress.getLocalHost()), pack);
            System.out.print(args[i]
                             + "\n  " + event
                             + "\n");
        }
    }
}
