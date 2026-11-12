package zm.ac.mu.registry.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import zm.ac.mu.registry.data.Data.User;

@Component
public class RoleInterceptor implements HandlerInterceptor {
    private final ObjectMapper mapper;
    public RoleInterceptor(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod method)) return true;
        Roles roles = method.getMethodAnnotation(Roles.class);
        if (roles == null) roles = method.getBeanType().getAnnotation(Roles.class);
        if (roles == null) return true;
        Object principal = request.getAttribute(SessionInterceptor.USER_ATTRIBUTE);
        if (!(principal instanceof User user)
                || Arrays.stream(roles.value()).noneMatch(role -> role == user.role())) {
            ProblemWriter.write(response, mapper, HttpStatus.FORBIDDEN,
                    "The authenticated user does not have permission for this operation.", List.of());
            return false;
        }
        return true;
    }
}
