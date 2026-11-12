package zm.ac.mu.registry.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public final class ProblemWriter {
    private ProblemWriter() {}

    public static void write(HttpServletResponse response, ObjectMapper mapper, HttpStatus status,
                             String detail, List<Problem.FieldError> errors) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new Problem("about:blank", status.getReasonPhrase(),
                status.value(), detail, errors == null ? List.of() : errors));
    }
}
