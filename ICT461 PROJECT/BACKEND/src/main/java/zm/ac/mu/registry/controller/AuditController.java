package zm.ac.mu.registry.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zm.ac.mu.registry.data.Data.Role;
import zm.ac.mu.registry.dto.AuditEventResponse;
import zm.ac.mu.registry.dto.PageResponse;
import zm.ac.mu.registry.service.AuditService;
import zm.ac.mu.registry.web.Roles;

@RestController
@RequestMapping("/api/v1/audit-events")
public class AuditController {
    private final AuditService audit;
    public AuditController(AuditService audit) { this.audit = audit; }

    @Roles(Role.AUDITOR)
    @GetMapping
    public PageResponse<AuditEventResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return audit.page(page, size);
    }
}
