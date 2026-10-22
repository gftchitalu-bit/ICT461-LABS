package zm.ac.mu.registry.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;
import zm.ac.mu.registry.dto.*;
import zm.ac.mu.registry.service.AuditService;

@RestController
@RequestMapping("/api/v1/audit-events")
public class AuditController {

    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping // Auditor only
    public PageResponse<AuditEventResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return audit.events(page, size);
    }
}