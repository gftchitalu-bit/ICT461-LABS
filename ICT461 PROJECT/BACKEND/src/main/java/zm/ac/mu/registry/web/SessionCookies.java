package zm.ac.mu.registry.web;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SessionCookies {
    private static final Duration TTL = Duration.ofMinutes(15);
    private final boolean secure;

    public SessionCookies(@Value("${app.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    public ResponseCookie issue(String sessionId) {
        return base(sessionId).maxAge(TTL).build();
    }

    public ResponseCookie expire() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from("session_id", value)
                .httpOnly(true).sameSite("Strict").path("/").secure(secure);
    }
}
