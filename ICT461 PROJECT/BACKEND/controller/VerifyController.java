package zm.ac.mu.registry.controller;

import org.springframework.web.bind.annotation.*;
import zm.ac.mu.registry.dto.VerificationResponse;
import zm.ac.mu.registry.service.VerificationService;

@RestController
@RequestMapping("/api/v1/verify")
public class VerifyController {

    private final VerificationService verification;

    public VerifyController(VerificationService verification) {
        this.verification = verification;
    }

    @GetMapping("/{token}") // GET = safe: it only reads, it never changes anything
    public VerificationResponse verify(@PathVariable String token) {
        return verification.verify(token);
    }
}