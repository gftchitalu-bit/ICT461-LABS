package zm.ac.mu.registry.service;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import zm.ac.mu.registry.data.Data.Credential;
import zm.ac.mu.registry.data.Data.CredentialStatus;
import zm.ac.mu.registry.data.Data.Revocation;
import zm.ac.mu.registry.data.Data.RevocationRepository;
import zm.ac.mu.registry.data.Data.RevocationStatus;
import zm.ac.mu.registry.data.Data.User;
import zm.ac.mu.registry.dto.RevocationRequest;
import zm.ac.mu.registry.dto.RevocationResponse;
import zm.ac.mu.registry.web.ApiFailure;

@Service
public class RevocationService {
    private final CredentialService credentials;
    private final RevocationRepository revocations;
    private final AuditService audit;

    public RevocationService(CredentialService credentials, RevocationRepository revocations, AuditService audit) {
        this.credentials = credentials;
        this.revocations = revocations;
        this.audit = audit;
    }

    public RevocationResponse request(long credentialId, RevocationRequest request, User registrar) {
        Credential credential = credentials.require(credentialId);
        if (credential.status() != CredentialStatus.ACTIVE)
            throw new ApiFailure(HttpStatus.CONFLICT, "Only ACTIVE credentials can be revoked.");
        Revocation saved = revocations.savePendingIfNone(new Revocation(0, credentialId, registrar.id(),
                request.reason(), RevocationStatus.PENDING, Instant.now(), null));
        if (saved == null)
            throw new ApiFailure(HttpStatus.CONFLICT, "Only ACTIVE credentials without a pending revocation can be revoked.");
        audit.record(registrar.id(), "REVOCATION_REQUESTED", "REVOCATION", saved.id(),
                "Credential " + credentialId);
        return RevocationResponse.from(saved);
    }

    public RevocationResponse approve(long revocationId, User approver) {
        Revocation current = revocations.findRevocation(revocationId)
                .orElseThrow(() -> new ApiFailure(HttpStatus.NOT_FOUND, "Revocation request not found."));
        if (current.requestedBy() == approver.id())
            throw new ApiFailure(HttpStatus.FORBIDDEN, "A different registrar must approve the request.");
        if (current.status() != RevocationStatus.PENDING)
            throw new ApiFailure(HttpStatus.CONFLICT, "Revocation request has already been approved.");
        try {
            Revocation approved = revocations.approveAtomically(revocationId, approver.id(), Instant.now());
            if (approved == null) throw new ApiFailure(HttpStatus.NOT_FOUND, "Revocation request not found.");
            audit.record(approver.id(), "REVOCATION_APPROVED", "REVOCATION", approved.id(),
                    "Credential " + approved.credentialId() + " revoked");
            return RevocationResponse.from(approved);
        } catch (IllegalStateException exception) {
            if (exception.getMessage().equals("REVOCATION_NOT_PENDING"))
                throw new ApiFailure(HttpStatus.CONFLICT, "Revocation request has already been approved.");
            if (exception.getMessage().equals("CREDENTIAL_NOT_ACTIVE"))
                throw new ApiFailure(HttpStatus.CONFLICT, "Credential is not ACTIVE.");
            throw exception;
        }
    }
}
