
public class WithException extends Ellapsed {

    public Object randomError() throws Exception {
        if (random.nextDouble() < probability) {
            throw new Exception();
        }
        return new Object();
    }

}
