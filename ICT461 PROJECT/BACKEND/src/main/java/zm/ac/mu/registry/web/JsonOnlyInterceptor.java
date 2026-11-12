package zm.ac.mu.registry.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JsonOnlyInterceptor implements HandlerInterceptor {
    private final ObjectMapper mapper;
    public JsonOnlyInterceptor(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String method = request.getMethod();
        if (method.equals("GET") || method.equals("HEAD") || method.equals("OPTIONS")) return true;
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(request.getContentType() == null ? "" : request.getContentType());
        } catch (IllegalArgumentException exception) {
            contentType = null;
        }
        if (contentType != null && contentType.getType().equalsIgnoreCase("application")
                && contentType.getSubtype().equalsIgnoreCase("json")) return true;
        ProblemWriter.write(response, mapper, HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type must be application/json.", List.of());
        return false;
    }
}
