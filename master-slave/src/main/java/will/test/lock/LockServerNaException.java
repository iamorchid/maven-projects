package will.test.lock;

/**
 * Lock server not available exception
 */
public class LockServerNaException extends Exception {

    public LockServerNaException(Throwable error) {
        super(error);
    }

}
