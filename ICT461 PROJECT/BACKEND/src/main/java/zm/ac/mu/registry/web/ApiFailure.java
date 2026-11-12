package zm.ac.mu.registry.web;

import org.springframework.http.HttpStatus;

public class ApiFailure extends RuntimeException {
    private final HttpStatus status;
    private final String field;

    public ApiFailure(HttpStatus status, String detail) {
        this(status, detail, null);
    }

    public ApiFailure(HttpStatus status, String detail, String field) {
        super(detail);
        this.status = status;
        this.field = field;
    }

    public HttpStatus status() { return status; }
    public String field() { return field; }
}
