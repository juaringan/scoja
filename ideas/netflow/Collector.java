import java.io.*;
import java.nio.*;
import java.net.*;

/**
 * http://netflow.caligare.com/netflow_v5.htm
 */
public class Collector {
    public static void main(final String[] args)
    throws Exception {
        int argc = 0;
        final int port = Integer.parseInt(args[argc++]);
        
        final DatagramSocket socket = new DatagramSocket(port);
        final byte[] buffer = new byte[64*1024];
        final ByteBuffer bb = ByteBuffer.wrap(buffer)
            .order(ByteOrder.BIG_ENDIAN);
        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        for (;;) {
            socket.receive(packet);
            bb.position(0).limit(packet.getLength());
            processMessage(bb);
        }
    }
    
    static void processMessage(final ByteBuffer bb) {
        System.err.println("Packet received; size=" + bb.limit());
        final int version = bb.getShort();
        System.err.println("Version: " + version);
        switch (version) {
        case 5: processV5(bb); break;
        case 7: processV7(bb); break;
        default:
            System.err.println("Unknown version");
        }
    }
    
    static final int V5_HEADER_SIZE = 24;
    static final int V5_RECORD_SIZE = 48;
    
    static void processV5(final ByteBuffer bb) {
        final int flows = bb.getShort();
        final int uptime = bb.getInt();
        final int secs = bb.getInt();
        final int nanos = bb.getInt();
        final int seq = bb.getInt();
        final int engineType = bb.get() & 0xFF;
        final int engineId = bb.get() & 0xFF;
        final int sampling = bb.getShort() & 0xFFFF;
        System.err.println(
            "Flows: " + flows
            + ", uptime: " + uptime
            + ", secs: " + secs
            + ", nanos: " + nanos
            + ", seq: " + seq
            + ", engine type: " + engineType
            + ", engine id: " + engineId
            + ", sampling: " + sampling);
        final int expectedSize = V5_HEADER_SIZE + flows*V5_RECORD_SIZE;
        if (expectedSize != bb.limit())
            System.err.println("Unexpected size");
        final int efflows = (bb.limit() - V5_HEADER_SIZE) / V5_RECORD_SIZE;
        for (int i = 0; i < efflows; i++) {
            bb.position(V5_HEADER_SIZE + i*V5_RECORD_SIZE);
            final int srcip = bb.getInt();
            final int dstip = bb.getInt();
            final int nextip = bb.getInt();
            final int iii = bb.getShort() & 0xFFFF;
            final int ioi = bb.getShort() & 0xFFFF;
            final int packets = bb.getInt();
            final int bytes = bb.getInt();
            final int firstTime = bb.getInt();
            final int lastTime = bb.getInt();
            final int srcport = bb.getShort() & 0xFFFF;
            final int dstport = bb.getShort() & 0xFFFF;
            bb.get(); //padding
            final int tcpFlags = bb.get() & 0xFF;
            final int protocol = bb.get() & 0xFF;
            final int tos = bb.get() & 0xFF;
            final int srcAS = bb.getShort() & 0xFFFF;
            final int dstAS = bb.getShort() & 0xFFFF;
            final int srcMask = bb.get();
            final int dstMask = bb.get();
            System.err.println(
                "src ip: " + dottedIP(srcip)
                + ", dst ip: " + dottedIP(dstip)
                + ", next ip: " + nextip
                + ", iii: " + iii
                + ", ioi: " + ioi
                + ", packets: " + packets
                + ", bytes: " + bytes
                + ", first time: " + firstTime
                + ", last time: " + lastTime
                + ", src port: " + srcport
                + ", dst port: " + dstport
                + ", tcp flags: " + tcpFlags
                + ", protocol: " + protocol
                + ", tos: " + tos
                + ", src as: " + srcAS
                + ", dst as: " + dstAS
                + ", src mask: " + srcMask
                + ", dst mask: " + dstMask);
        }
    }
    
    static void processV7(final ByteBuffer bb) {
        
    }
    
    static String dottedIP(final int ip) {
        return (ip >>> 24)
            + "." + ((ip >>> 16) & 0xFF)
            + "." + ((ip >>> 8) & 0xFF)
            + "." + (ip & 0xFF);
    }
}
