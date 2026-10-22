package zm.ac.mu.registry.service;

import zm.ac.mu.registry.dto.VerificationResponse;

public interface VerificationService {
    VerificationResponse verify(String token);
}