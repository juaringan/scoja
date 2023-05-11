
public class NativeInc {
    static {
        System.out.println("Just loading inc library");
        System.loadLibrary("inc");
        System.out.println("Library inc loaded");
    }
    
    public static native int inc(int n);
    
    public String toString() {
        return "inc(0) = " + inc(0);
    }
}
