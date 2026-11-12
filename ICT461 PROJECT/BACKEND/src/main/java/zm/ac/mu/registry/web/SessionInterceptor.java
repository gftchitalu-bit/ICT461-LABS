package zm.ac.mu.registry.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import zm.ac.mu.registry.data.Data.Session;
import zm.ac.mu.registry.data.Data.User;
import zm.ac.mu.registry.data.Data.SessionRepository;
import zm.ac.mu.registry.data.Data.UserRepository;

@Component
public class SessionInterceptor implements HandlerInterceptor {
    public static final String USER_ATTRIBUTE = SessionInterceptor.class.getName() + ".user";
    public static final String SESSION_HASH_ATTRIBUTE = SessionInterceptor.class.getName() + ".sessionHash";
    private final SessionRepository sessions;
    private final UserRepository users;
    private final ObjectMapper mapper;

    public SessionInterceptor(SessionRepository sessions, UserRepository users, ObjectMapper mapper) {
        this.sessions = sessions;
        this.users = users;
        this.mapper = mapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (isPublic(handler)) return true;
        String rawId = request.getCookies() == null ? null : Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals("session_id"))
                .map(Cookie::getValue).findFirst().orElse(null);
        if (rawId == null || rawId.isBlank()) return unauthorized(response);
        String hash = Hashing.sha256(rawId);
        Session session = sessions.findSession(hash).orElse(null);
        if (session == null || session.revoked() || !session.expiresAt().isAfter(Instant.now()))
            return unauthorized(response);
        User user = users.findUser(session.userId()).filter(User::active).orElse(null);
        if (user == null) return unauthorized(response);
        request.setAttribute(USER_ATTRIBUTE, user);
        request.setAttribute(SESSION_HASH_ATTRIBUTE, hash);
        return true;
    }

    private boolean unauthorized(HttpServletResponse response) throws IOException {
        ProblemWriter.write(response, mapper, HttpStatus.UNAUTHORIZED,
                "Authentication is required.", List.of());
        return false;
    }

    private boolean isPublic(Object handler) {
        if (!(handler instanceof HandlerMethod method)) return false;
        return method.hasMethodAnnotation(Public.class) || method.getBeanType().isAnnotationPresent(Public.class);
    }
}
