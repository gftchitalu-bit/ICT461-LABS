package zm.ac.mu.registry.service;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import zm.ac.mu.registry.data.Data.Credential;
import zm.ac.mu.registry.data.Data.CredentialRepository;
import zm.ac.mu.registry.data.Data.CredentialStatus;
import zm.ac.mu.registry.data.Data.ShareLink;
import zm.ac.mu.registry.data.Data.ShareLinkRepository;
import zm.ac.mu.registry.dto.VerificationResponse;
import zm.ac.mu.registry.web.ApiFailure;
import zm.ac.mu.registry.web.Hashing;

@Service
public class VerificationService {
    private final ShareLinkRepository links;
    private final CredentialRepository credentials;

    public VerificationService(ShareLinkRepository links, CredentialRepository credentials) {
        this.links = links;
        this.credentials = credentials;
    }

    public VerificationResponse verify(String token) {
        ShareLink link = links.findShareLink(Hashing.sha256(token))
                .orElseThrow(() -> new ApiFailure(HttpStatus.NOT_FOUND, "Share token not found."));
        Credential credential = credentials.findCredential(link.credentialId())
                .orElseThrow(() -> new ApiFailure(HttpStatus.NOT_FOUND, "Credential not found."));
        if (credential.status() == CredentialStatus.REVOKED)
            return VerificationResponse.unavailable("REVOKED");
        if (!link.expiresAt().isAfter(Instant.now()))
            return VerificationResponse.unavailable("EXPIRED");
        return VerificationResponse.active(credential.programme(), credential.awardDate());
    }
}
