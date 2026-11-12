package zm.ac.mu.registry.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private record Window(long minute, int count) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final int defaultLimit;
    private final int loginLimit;

    public RateLimitInterceptor(ObjectMapper mapper,
            @Value("${app.rate-limit.default:120}") int defaultLimit,
            @Value("${app.rate-limit.login:10}") int loginLimit) {
        this.mapper = mapper;
        this.defaultLimit = defaultLimit;
        this.loginLimit = loginLimit;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        long minute = Instant.now().getEpochSecond() / 60;
        int limit = request.getRequestURI().endsWith("/auth/login") ? loginLimit : defaultLimit;
        String category = request.getRequestURI().endsWith("/auth/login") ? "login" : "default";
        String key = request.getRemoteAddr() + ':' + category;
        Window window = windows.compute(key, (ignored, current) ->
                current == null || current.minute() != minute
                        ? new Window(minute, 1) : new Window(minute, current.count() + 1));
        int remaining = Math.max(0, limit - window.count());
        long reset = (minute + 1) * 60;
        response.setHeader("X-RateLimit-Limit", Integer.toString(limit));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(remaining));
        response.setHeader("X-RateLimit-Reset", Long.toString(reset));
        if (window.count() <= limit) return true;
        response.setHeader("Retry-After", Long.toString(Math.max(1, reset - Instant.now().getEpochSecond())));
        ProblemWriter.write(response, mapper, HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded.", List.of());
        return false;
    }
}
