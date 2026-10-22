package zm.ac.mu.registry.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import zm.ac.mu.registry.dto.*;
import zm.ac.mu.registry.service.*;

@RestController
@RequestMapping("/api/v1/credentials")
public class CredentialController {

    private final CredentialService credentials;
    private final RevocationService revocations;
    private final ShareLinkService shareLinks;

    public CredentialController(CredentialService credentials,
                                RevocationService revocations,
                                ShareLinkService shareLinks) {
        this.credentials = credentials;
        this.revocations = revocations;
        this.shareLinks = shareLinks;
    }

    // Registrar only (Christopher adds @PreAuthorize("hasRole('REGISTRAR')") later)
    @PostMapping
    public ResponseEntity<CredentialResponse> issue(@Valid @RequestBody CredentialRequest request) {
        CredentialResponse created = credentials.issue(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created); // 201 + Location header
    }

    // Registrar, Auditor
    @GetMapping
    public PageResponse<CredentialResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @Pattern(regexp = "ACTIVE|REVOKED") String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return credentials.search(q, status, page, size); // 200 is the default
    }

    // Registrar, or the Graduate who owns it (object-level check lives in the service/security)
    @GetMapping("/{id}")
    public CredentialResponse get(@PathVariable String id) {
        return credentials.get(id);
    }

    // Registrar A
    @PostMapping("/{id}/revocations")
    public ResponseEntity<RevocationResponse> requestRevocation(
            @PathVariable String id, @Valid @RequestBody RevocationRequest request) {
        RevocationResponse created = revocations.request(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/revocations/{rid}").buildAndExpand(created.revocationId()).toUri();
        return ResponseEntity.created(location).body(created); // 201: a PENDING request was created
    }

    // Graduate
    @PostMapping("/{id}/share-links")
    public ResponseEntity<ShareLinkResponse> createShareLink(
            @PathVariable String id, @Valid @RequestBody ShareLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shareLinks.create(id, request));
    }
}