package zm.ac.mu.registry.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zm.ac.mu.registry.dto.EmptyRequest;
import zm.ac.mu.registry.data.Data.User;
import zm.ac.mu.registry.dto.LoginRequest;
import zm.ac.mu.registry.dto.UserResponse;
import zm.ac.mu.registry.service.AuthService;
import zm.ac.mu.registry.web.Public;
import zm.ac.mu.registry.web.SessionCookies;
import zm.ac.mu.registry.web.SessionInterceptor;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final SessionCookies cookies;

    public AuthController(AuthService auth, SessionCookies cookies) {
        this.auth = auth;
        this.cookies = cookies;
    }

    @Public
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = auth.login(request);
        ResponseCookie cookie = cookies.issue(result.sessionId());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(result.user());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) EmptyRequest body,
            HttpServletRequest request) {
        User user = (User) request.getAttribute(SessionInterceptor.USER_ATTRIBUTE);
        String hash = (String) request.getAttribute(SessionInterceptor.SESSION_HASH_ATTRIBUTE);
        auth.logout(user, hash);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.expire().toString()).build();
    }

    @GetMapping("/me")
    public UserResponse me(HttpServletRequest request) {
        return UserResponse.from((User) request.getAttribute(SessionInterceptor.USER_ATTRIBUTE));
    }
}
