package zm.ac.mu.registry.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import zm.ac.mu.registry.data.Data.Role;
import zm.ac.mu.registry.data.Data.User;
import zm.ac.mu.registry.dto.CredentialRequest;
import zm.ac.mu.registry.dto.CredentialResponse;
import zm.ac.mu.registry.dto.PageResponse;
import zm.ac.mu.registry.dto.RevocationRequest;
import zm.ac.mu.registry.dto.RevocationResponse;
import zm.ac.mu.registry.dto.ShareLinkRequest;
import zm.ac.mu.registry.dto.ShareLinkResponse;
import zm.ac.mu.registry.service.CredentialService;
import zm.ac.mu.registry.service.RevocationService;
import zm.ac.mu.registry.service.ShareLinkService;
import zm.ac.mu.registry.web.Roles;
import zm.ac.mu.registry.web.SessionInterceptor;

@RestController
@RequestMapping("/api/v1/credentials")
public class CredentialController {
    private static final String SEARCH_PATTERN = "^[A-Za-z0-9 .,&()'/-]+$";
    private final CredentialService credentials;
    private final RevocationService revocations;
    private final ShareLinkService shareLinks;

    public CredentialController(CredentialService credentials, RevocationService revocations,
            ShareLinkService shareLinks) {
        this.credentials = credentials;
        this.revocations = revocations;
        this.shareLinks = shareLinks;
    }

    @Roles(Role.REGISTRAR)
    @PostMapping
    public ResponseEntity<CredentialResponse> issue(@Valid @RequestBody CredentialRequest body,
            HttpServletRequest request) {
        CredentialResponse created = credentials.issue(body, currentUser(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Roles({Role.REGISTRAR, Role.AUDITOR})
    @GetMapping
    public PageResponse<CredentialResponse> search(
            @RequestParam(required = false) @Size(max = 100) @Pattern(regexp = SEARCH_PATTERN) String q,
            @RequestParam(required = false) @Pattern(regexp = "^(ACTIVE|REVOKED)$") String status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return credentials.search(q, status, page, size);
    }

    @Roles({Role.REGISTRAR, Role.GRADUATE})
    @GetMapping("/{id}")
    public CredentialResponse get(@PathVariable @Min(1) long id, HttpServletRequest request) {
        return credentials.get(id, currentUser(request));
    }

    @Roles(Role.REGISTRAR)
    @PostMapping("/{id}/revocations")
    public ResponseEntity<RevocationResponse> requestRevocation(@PathVariable @Min(1) long id,
            @Valid @RequestBody RevocationRequest body, HttpServletRequest request) {
        RevocationResponse created = revocations.request(id, body, currentUser(request));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/revocations/{id}/approvals").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Roles(Role.GRADUATE)
    @PostMapping("/{id}/share-links")
    public ResponseEntity<ShareLinkResponse> createShareLink(@PathVariable @Min(1) long id,
            @Valid @RequestBody ShareLinkRequest body, HttpServletRequest request) {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        ShareLinkResponse created = shareLinks.create(id, body, currentUser(request), baseUrl);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/verify/{token}").buildAndExpand(created.token()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    private User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute(SessionInterceptor.USER_ATTRIBUTE);
    }
}
