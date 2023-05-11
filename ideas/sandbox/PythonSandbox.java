
import java.io.*;
import org.python.util.PythonInterpreter;

public class PythonSandbox {
    
    public static void main(final String[] args)
    throws Exception {
        System.getProperties();
        final PythonInterpreter interp = new PythonInterpreter();
        for (final String script: args) interp.execfile(script);
    }
}
