package zm.ac.mu.registry.dto;

import java.time.Instant;
import zm.ac.mu.registry.data.Data.Revocation;

public record RevocationResponse(long id, long credentialId, long requestedBy, String reason,
                                 String status, Instant createdAt, Long approvedBy) {
    public static RevocationResponse from(Revocation revocation) {
        return new RevocationResponse(revocation.id(), revocation.credentialId(), revocation.requestedBy(),
                revocation.reason(), revocation.status().name(), revocation.createdAt(), revocation.approvedBy());
    }
}
