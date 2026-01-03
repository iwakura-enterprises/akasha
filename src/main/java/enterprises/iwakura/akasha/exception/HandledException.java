package enterprises.iwakura.akasha.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HandledException extends RuntimeException {

    private boolean notFound;

    public HandledException(String message, Exception cause) {
        super(message, cause);
    }

    public HandledException asNotFound() {
        this.notFound = true;
        return this;
    }
}
