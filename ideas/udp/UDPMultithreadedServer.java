
import java.net.*;

public class UDPMultithreadedServer extends Thread {

    public static void main(final String[] args) throws Exception {
        int argc = 0;
        final int port = Integer.parseInt(args[argc++]);
        final int threads = Integer.parseInt(args[argc++]);
        final DatagramSocket dsocket = new DatagramSocket(port);
        final UDPMultithreadedServer[] servers
            = new UDPMultithreadedServer[threads];
        for (int i = 0; i < threads; i++) {
            servers[i] = new UDPMultithreadedServer(dsocket, i);
            servers[i].start();
        }
    }
    
    public static final int DPACKET_SIZE = 100000;
    
    private final DatagramSocket dsocket;
    private final int id;
    private final DatagramPacket packet;
    
    public UDPMultithreadedServer(final DatagramSocket dsocket,
                                  final int id) {
        this.dsocket = dsocket;
        this.id = id;
        this.packet = new DatagramPacket(new byte[DPACKET_SIZE], DPACKET_SIZE);
    }
    
    public void run() {
        for (;;) {
            try {
                dsocket.receive(packet);
                print(packet);
            } catch (Exception e) {
                System.err.println("Error at thread " + id);
                e.printStackTrace(System.err);
            }
        }
    }
    
    private void print(final DatagramPacket packet) {
        System.out.print("Packet received at thread " + id 
                         + " from " + packet.getAddress()
                         + " size " + packet.getLength()
                         + "\n  ");
        final byte[] data = packet.getData();
        final int len = packet.getLength();
        /*
        for (int i = 0; i < len; i++) {
            System.out.print((char)data[i]);
        }
        System.out.println();
        */
    }
}
