
package org.scoja.util;

import java.io.Serializable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class UNIXSocketSerializationTest {

    public final UNIXAddress addr = new UNIXAddress("hola.socket");

    final class ObjectSerializable implements Serializable {

	final String msg = "message serialized? :-) ";


	public final void hello() {
	    System.out.println(this.msg);
	}
    }

    final class ClientSerialization implements Runnable {
	public void run() {
	    try {
		final UNIXSocket cli = new UNIXSocket();
		cli.connect(addr);

		OutputStream os = cli.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);

		//ObjectSerializable obj = new ObjectSerializable();
		String obj = new String("Java.nullable.exception.irrecuperable kernel panic (please halt your computer immediately)");
		oos.writeObject(obj);
		oos.flush();
		oos.close();

	    } catch (Exception e) {
		System.out.println("");
		System.out.println("");
		System.out.println("client =>");
		e.printStackTrace();
	    }
	}
    }


    final class ServerSerialization implements Runnable {
	public void run() {


	    try {
		final UNIXServerSocket ser = new UNIXServerSocket();
		ser.bind(addr);
		ser.listen(10);

		final UNIXSocket cli_addr = ser.accept();

		InputStream is = cli_addr.getInputStream();

		ObjectInputStream ois = new ObjectInputStream(is);

		//ObjectSerializable obj = (ObjectSerializable) ois.readObject();
		String obj = (String) ois.readObject();
		ois.close();

		//obj.hello();
		System.out.println(obj);

	    } catch (Exception e) {
		System.out.println("");
		System.out.println("");
		System.out.println("server =>");
		e.printStackTrace();
	    }
	}
    }

    public UNIXSocketSerializationTest() {
	Thread ser = new Thread(new ServerSerialization());
	Thread cli = new Thread(new ClientSerialization());


	ser.start();
	try {
	    Thread.currentThread().sleep(1000);
	} catch (InterruptedException e) {}
	cli.start();

    }


    public static void main(String[] argv) {
	new UNIXSocketSerializationTest();
    }

}
