package zm.ac.mu.registry.dto;

import java.time.Instant;
import java.time.LocalDate;
import zm.ac.mu.registry.data.Data.Credential;

public record CredentialResponse(long id, long graduateId, String programme, LocalDate awardDate,
                                 String status, Instant issuedAt) {
    public static CredentialResponse from(Credential credential) {
        return new CredentialResponse(credential.id(), credential.graduateId(), credential.programme(),
                credential.awardDate(), credential.status().name(), credential.issuedAt());
    }
}
