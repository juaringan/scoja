import java.io.*;

public class OpenFileP implements OpenFile {
    public InputStream open(final String name)
    throws IOException {
        return new FileInputStream(name);
    }
}
