
import java.io.*;
import java.net.*;

public class SimpleReadFromDatagram extends Thread {

    public static void main(final String[] args) throws Exception {
        int argc = 0;
        final int port = Integer.parseInt(args[argc++]);
        final int n = Integer.parseInt(args[argc++]);
        
        final DatagramSocket socket = new DatagramSocket(port);
        final DatagramPacket packet
            = new DatagramPacket(new byte[2*1024], 2*1024);
        final Thread[] ts = new Thread[n];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread() {
                    public void run() {
                        try {
                            for (;;) {
                                socket.receive(packet);
                                System.out.println(packet);
                            }
                        } catch (Exception e) {
                            e.printStackTrace(System.out);
                        }
                    }
                };
            ts[i].start();
        }
        try {
            Thread.sleep(2*1000);
            System.out.println("A cerrar");
            socket.close();
            /*
            for (int i = 0; i < ts.length; i++) {
                ts[i].interrupt();
            }
            */
            System.out.println("Cerrado");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
