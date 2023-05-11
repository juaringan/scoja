
public class Task implements Runnable {


    public static void main(final String[] args) {
        final Task task = new Task();
        
        final EnvThread thread1 = new EnvThread(task);
        thread1.put("key", "value1");
        thread1.start();
        
        final EnvThread thread2 = new EnvThread(task);
        thread2.put("key", "value2");
        thread2.start();
    }
    
    public void run() {
        try {
            for (;;) {
                final EnvThread thread = (EnvThread)Thread.currentThread();
                System.out.println(thread.get("key"));
                Thread.sleep(1000);
            }
        } catch (Exception e) {}
    }

}
