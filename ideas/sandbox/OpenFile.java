import java.io.*;

public interface OpenFile {
    public InputStream open(String name) throws IOException;
}
