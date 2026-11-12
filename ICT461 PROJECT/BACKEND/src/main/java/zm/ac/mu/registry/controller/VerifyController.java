package zm.ac.mu.registry.controller;

import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zm.ac.mu.registry.dto.VerificationResponse;
import zm.ac.mu.registry.service.VerificationService;
import zm.ac.mu.registry.web.Public;

@RestController
@RequestMapping("/api/v1/verify")
public class VerifyController {
    private final VerificationService verification;
    public VerifyController(VerificationService verification) { this.verification = verification; }

    @Public
    @GetMapping("/{token}")
    public VerificationResponse verify(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9_-]{43}$") String token) {
        return verification.verify(token);
    }
}
