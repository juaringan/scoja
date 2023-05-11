
package org.scoja.util;

import java.net.SocketException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class UNIXSocketTest {

    public final UNIXAddress addr = new UNIXAddress("tmp.socket");

    final class Client implements Runnable {
	public void run() {
	    try {
		final UNIXSocket cli = new UNIXSocket();
		cli.connect(addr);

		OutputStream os = cli.getOutputStream();
		os.write(new String("hola mundo").getBytes());
		Thread.currentThread().sleep(1000);
		os.write(new String("hola mundo").getBytes());
		os.close();
	    } catch (Exception e) {
		System.out.println("");
		System.out.println("");
		System.out.println("client =>");
		e.printStackTrace();
	    }
	}
    }


    final class Server implements Runnable {
	public void run() {

	    byte msg[] = new byte[1024];

	    try {
		final UNIXServerSocket ser = new UNIXServerSocket();
		ser.bind(addr);
		ser.listen(10);

		UNIXSocket cli_addr = ser.accept();
		InputStream is = cli_addr.getInputStream();

		int res;
		while((res = is.read(msg)) != -1)
		    System.out.println(new String(msg,0,res));

	    } catch (Exception e) {
		System.out.println("");
		System.out.println("");
		System.out.println("server =>");
		e.printStackTrace();
	    }
	}
    }

    public UNIXSocketTest() {
	Thread ser = new Thread(new Server());
	Thread cli = new Thread(new Client());


	ser.start();
	try {
	    Thread.currentThread().sleep(1000);
	} catch (InterruptedException e) {}
	cli.start();

    }


    public static void main(String[] argv) {
	new UNIXSocketTest();
    }

}
