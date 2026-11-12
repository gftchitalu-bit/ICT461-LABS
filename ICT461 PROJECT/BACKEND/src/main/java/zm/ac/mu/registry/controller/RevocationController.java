package zm.ac.mu.registry.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zm.ac.mu.registry.data.Data.Role;
import zm.ac.mu.registry.data.Data.User;
import zm.ac.mu.registry.dto.EmptyRequest;
import zm.ac.mu.registry.dto.RevocationResponse;
import zm.ac.mu.registry.service.RevocationService;
import zm.ac.mu.registry.web.Roles;
import zm.ac.mu.registry.web.SessionInterceptor;

@RestController
@RequestMapping("/api/v1/revocations")
public class RevocationController {
    private final RevocationService revocations;

    public RevocationController(RevocationService revocations) { this.revocations = revocations; }

    @Roles(Role.REGISTRAR)
    @PostMapping("/{revocationId}/approvals")
    public RevocationResponse approve(@PathVariable @Min(1) long revocationId,
            @RequestBody(required = false) EmptyRequest body, HttpServletRequest request) {
        User approver = (User) request.getAttribute(SessionInterceptor.USER_ATTRIBUTE);
        return revocations.approve(revocationId, approver);
    }
}
