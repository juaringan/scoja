import java.io.*;

public class OpenFileU implements OpenFile {
    public InputStream open(final String name)
    throws IOException {
        return new FileInputStream(name);
    }
}
