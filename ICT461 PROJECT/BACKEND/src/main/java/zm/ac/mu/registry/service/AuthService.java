package zm.ac.mu.registry.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import zm.ac.mu.registry.data.Data.Session;
import zm.ac.mu.registry.data.Data.SessionRepository;
import zm.ac.mu.registry.data.Data.User;
import zm.ac.mu.registry.data.Data.UserRepository;
import zm.ac.mu.registry.dto.LoginRequest;
import zm.ac.mu.registry.dto.UserResponse;
import zm.ac.mu.registry.web.ApiFailure;
import zm.ac.mu.registry.web.Hashing;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final SessionRepository sessions;
    private final AuditService audit;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String dummyHash;

    public AuthService(UserRepository users, SessionRepository sessions, AuditService audit) {
        this.users = users;
        this.sessions = sessions;
        this.audit = audit;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.dummyHash = passwordEncoder.encode("not-a-real-user-password");
    }

    public LoginResult login(LoginRequest request) {
        User user = users.findByEmail(request.email()).orElse(null);
        String hash = user == null ? dummyHash : user.passwordHash();
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || !user.active() || !matches) {
            audit.record(user == null ? null : user.id(), "LOGIN_FAILURE", "USER",
                    user == null ? null : user.id(), "Invalid credentials");
            throw new ApiFailure(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String sessionId = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        sessions.saveSession(new Session(Hashing.sha256(sessionId), user.id(),
                Instant.now().plus(15, ChronoUnit.MINUTES), false));
        audit.record(user.id(), "LOGIN_SUCCESS", "USER", user.id(), "Session created");
        return new LoginResult(UserResponse.from(user), sessionId);
    }

    public void logout(User user, String sessionHash) {
        sessions.revokeSession(sessionHash);
        audit.record(user.id(), "LOGOUT", "USER", user.id(), "Session revoked");
    }

    public record LoginResult(UserResponse user, String sessionId) {}
}
