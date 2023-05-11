
import java.io.*;
import java.net.*;

public class ReadFromDatagram extends Thread {

    public static void main(final String[] args) throws Exception {
        int argc = 0;
        final int port = Integer.parseInt(args[argc++]);
        final DatagramSocket socket = new DatagramSocket(port);
        
        final Thread t[] = new Thread[2];
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        for (int i = 0; i < t.length; i++) {
            t[i] = new ReadFromDatagram(socket);
            t[i].start();
        }
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        /*
        for (int i = 0; i < t.length; i++) {
            t[i].interrupt();
        }
        */
        socket.close();
    }
    
    protected final DatagramSocket socket;
    
    public ReadFromDatagram(final DatagramSocket socket) {
        this.socket = socket;
    }
    
    public void run() {
        final DatagramPacket packet
            = new DatagramPacket(new byte[2*1024], 2*1024);
        try {
            for (;;) {
                socket.receive(packet);
                System.out.println(packet);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
