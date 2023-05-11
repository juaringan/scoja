import java.io.*;

public class OpenFileLauncher {

    public static void main(final String[] args)
    throws Exception {
        final SecurityManager sm = System.getSecurityManager();
        System.out.println("Security manager: " + sm);
        final OpenFile opener = (OpenFile)Class.forName(args[0]).newInstance();
        System.out.println("Opener: " + opener);
        final InputStream file = opener.open("OpenFile.java");
        System.out.println("File: " + file);
        file.close();
    }
}
