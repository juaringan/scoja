
import java.io.*;

public class OneClassLoader extends ClassLoader {

    /*
    public static void main(final String[] args) throws Exception {
        int argc = 0;
        final String className = args[argc++];
        final String filename = args[argc++];
        final OneClassLoader l1 = new OneClassLoader(className, filename);
        final OneClassLoader l2 = new OneClassLoader(className, filename);
        final Class c1 = l1.loadClass(className);
        final Class c2 = l2.loadClass(className);
        System.out.println("Class 1: " + c1 + " " + c1.getClassLoader());
        System.out.println("Class 2: " + c2 + " " + c2.getClassLoader());
        System.out.println("Equals: " + (c1 == c2));
        final Object ni1 = c1.newInstance();
        final Object ni2 = c2.newInstance();
    }
    */
    public static void main(final String[] args) throws Exception {
        for (int i = 0; i < args.length; i+=2) {
            final String className = args[i];
            final String filename = args[i+1];
            final OneClassLoader l1 = new OneClassLoader(className, filename);
            final Class c1 = l1.loadClass(className);
            System.out.println("Class 1: " + c1 + " " + c1.getClassLoader());
            final Object ni1 = c1.newInstance();
            System.out.println("Object " + ni1);
        }
    }

    final String myClassName;
    final byte[] myClassData;
    
    public OneClassLoader(final String className, final String filename)
        throws IOException {
        final File file = new File(filename);
        this.myClassName = className;
        this.myClassData = new byte[(int)file.length()];
        final InputStream is = new FileInputStream(file);
        is.read(myClassData);
        is.close();
    }

    public Class findClass(String name) throws ClassNotFoundException {
        if (!name.equals(myClassName)) {
            throw new ClassNotFoundException(name);
        }
        return defineClass(myClassName, myClassData, 0, myClassData.length);
    }
}
