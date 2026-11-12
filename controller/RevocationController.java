package zm.ac.mu.registry.controller;

import org.springframework.web.bind.annotation.*;
import zm.ac.mu.registry.dto.RevocationResponse;
import zm.ac.mu.registry.service.RevocationService;

@RestController
@RequestMapping("/api/v1/revocations")
public class RevocationController {

    private final RevocationService revocations;

    public RevocationController(RevocationService revocations) {
        this.revocations = revocations;
    }

    // Registrar B. The service must reject "same person who requested it" (403)
    // and "already approved" (409).
    @PostMapping("/{revocationId}/approvals")
    public RevocationResponse approve(@PathVariable String revocationId) {
        return revocations.approve(revocationId); // 200: the revocation is now complete
    }
}