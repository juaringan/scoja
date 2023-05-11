class Unicode {
    static {
        System.loadLibrary("Unicode");
    }
    
    public void stringAtJava(final String str) {
    }

    public native void stringAtC(String str, byte[] bytes);
    
    public static void main(String[] args) {
        final Unicode uni = new Unicode();
        uni.stringAtJava(args[0]);
        uni.stringAtC(args[0], args[0].getBytes());
    }
}
