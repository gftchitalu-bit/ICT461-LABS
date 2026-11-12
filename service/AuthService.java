package zm.ac.mu.registry.service;

import zm.ac.mu.registry.dto.LoginRequest;

public interface AuthService {
    String login(LoginRequest request);
}