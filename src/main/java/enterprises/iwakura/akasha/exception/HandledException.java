package enterprises.iwakura.akasha.exception;

public class HandledException extends RuntimeException {

    public HandledException(String message, Exception cause) {
        super(message, cause);
    }
}
