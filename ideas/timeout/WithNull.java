
public class WithNull extends Ellapsed {

    public Object randomError() throws Exception {
        if (random.nextDouble() < probability) return null;
        return new Object();
    }

}
