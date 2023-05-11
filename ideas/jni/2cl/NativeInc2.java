
public class NativeInc2 {
    static {
        System.out.println("Just loading inc library");
        System.loadLibrary("incMain2");
        System.out.println("Library inc loaded");
    }
    
    public static native int inc(int n);
    
    public String toString() {
        return "inc(0) = " + inc(0);
    }
}
