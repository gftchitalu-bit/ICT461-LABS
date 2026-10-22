package zm.ac.mu.registry.service.stub;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import zm.ac.mu.registry.dto.*;
import zm.ac.mu.registry.exception.*;
import zm.ac.mu.registry.service.*;

@Service
@Profile("stub") // only exists when the "stub" profile is active
public class StubRegistryService implements CredentialService, RevocationService,
        ShareLinkService, VerificationService, AuditService, AuthService {

    @Override
    public String login(LoginRequest r) {
        return "stub-jwt-token";
    }

    @Override
    public CredentialResponse issue(CredentialRequest r) {
        return new CredentialResponse(UUID.randomUUID().toString(),
                r.graduateId(), r.programme(), r.awardDate(), "ACTIVE");
    }

    @Override
    public CredentialResponse get(String id) {
        if ("404".equals(id)) throw new NotFoundException("Credential " + id + " not found");
        return new CredentialResponse(id, "G-001", "BSc Computer Science",
                LocalDate.of(2026, 7, 1), "ACTIVE");
    }

    @Override
    public PageResponse<CredentialResponse> search(String q, String status, int page, int size) {
        return new PageResponse<>(List.of(get("demo-1")), page, size, 1);
    }

    @Override
    public RevocationResponse request(String credentialId, RevocationRequest r) {
        if ("revoked-1".equals(credentialId)) throw new ConflictException("Credential is already revoked");
        return new RevocationResponse(UUID.randomUUID().toString(), credentialId, "PENDING");
    }

    @Override
    public RevocationResponse approve(String revocationId) {
        return new RevocationResponse(revocationId, "demo-1", "APPROVED");
    }

    @Override
    public ShareLinkResponse create(String credentialId, ShareLinkRequest r) {
        String token = UUID.randomUUID().toString();
        return new ShareLinkResponse(token, "https://registry.mu.ac.zm/verify/" + token,
                "stub-qr-code", Instant.now().plus(r.expiresInHours(), ChronoUnit.HOURS));
    }

    @Override
    public VerificationResponse verify(String token) {
        if ("unknown".equals(token)) throw new NotFoundException("Token not found or expired");
        return new VerificationResponse(true, "ACTIVE", "BSc Computer Science", LocalDate.of(2026, 7, 1));
    }

    @Override
    public PageResponse<AuditEventResponse> events(int page, int size) {
        return new PageResponse<>(
                List.of(new AuditEventResponse("1", "CREDENTIAL_ISSUED", "registrar@mu.ac.zm", Instant.now())),
                page, size, 1);
    }
}