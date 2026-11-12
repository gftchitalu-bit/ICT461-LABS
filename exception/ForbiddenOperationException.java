package zm.ac.mu.registry.exception;

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) { super(message); }
}