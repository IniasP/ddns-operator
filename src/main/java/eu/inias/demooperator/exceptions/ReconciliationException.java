package eu.inias.demooperator.exceptions;

public class ReconciliationException extends RuntimeException {
    public ReconciliationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReconciliationException(String message) {
        super(message);
    }
}
