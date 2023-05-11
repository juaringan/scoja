
package org.scoja.server.source;

import org.scoja.server.core.ScojaThread;
import org.scoja.server.core.ClusterSkeleton;
import org.scoja.server.core.DecoratedLink;
import org.scoja.server.core.Linkable;
import org.scoja.server.core.Link;
import org.scoja.server.core.Event;
import org.scoja.server.core.EventContext;
import org.scoja.server.parser.ParsedEvent;

import org.scoja.util.UNIXSocketDatagram;
import org.scoja.util.UNIXAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.io.IOException;

public class UNIXUDPSource 
    extends ClusterSkeleton
    implements DecoratedLink, Runnable {

    public static final int DEFAULT_MAX_BUFFER_SIZE = 2000;

    protected final Link link;
    protected final InetAddress inetAddr;

    protected UNIXAddress unixAddr;
    protected String address;
    protected int maxPacketSize;

    protected UNIXSocketDatagram socket;

    public UNIXUDPSource() throws UnknownHostException {
        this.link = new Link();
	this.inetAddr = InetAddress.getLocalHost();
        this.socket = null;
    }

    public Linkable getLinkable() {
        return link;
    }
    
    public void setAddress(final String address) {
        this.address = address;
    }
    
    public void setMaxPacketSize(final int max) {
        this.maxPacketSize = max;
    }
    
    
    public void start() {
        System.out.println("Starting " + this);
	try {
	    socket = new UNIXSocketDatagram();
	    unixAddr = new UNIXAddress(address);

	    super.start();
	    super.startAllThreads();
	    socket.bind(unixAddr);
	} catch (SocketException se) {
	    System.err.println(se.getMessage());
	    System.err.println("Source " + this + " will not listen!");
	}
    }

    public void shouldStop() {
        super.shouldStop();
        if (socket != null) {
	    try {
		socket.close();
	    } catch (SocketException se) {}
        }
    }

    public void run() {
        final Thread thread = Thread.currentThread();
        if (!(thread instanceof ScojaThread)) {
            Internal.emerg(Internal.SOURCE_UNIX_DGRAM,
                           "Refusing to be executed by thread " + thread 
                           + " that isn't a ScojaThread.");
            return;
        }
        final ScojaThread sthread = (ScojaThread)thread;
	final byte[] data = new byte[maxPacketSize];

        while (!stopRequested()) {
            try {
		int res = socket.receive(data);
                final Event event
                    = new ParsedEvent(inetAddr,data, 0, res);
                final EventContext ectx = new EventContext(event);
                sthread.setEventContext(ectx);
                link.process(ectx);
            } catch (Throwable e) {
                e.printStackTrace(System.err);
                System.err.println("While receiving from " + socket
                                   + ": " + e.getMessage());
            }
        }
    }

    public String toString() {
        return "UNIX udp source listening at " + address;
    }

}
