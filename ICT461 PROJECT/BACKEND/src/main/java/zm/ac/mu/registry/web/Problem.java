package zm.ac.mu.registry.web;

import java.util.List;

public record Problem(String type, String title, int status, String detail, List<FieldError> errors) {
    public record FieldError(String field, String message) {}
}
