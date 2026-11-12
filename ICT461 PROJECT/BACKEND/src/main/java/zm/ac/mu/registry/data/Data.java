package zm.ac.mu.registry.data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class Data {
    private Data() {}

    public enum Role { REGISTRAR, GRADUATE, AUDITOR }
    public enum CredentialStatus { ACTIVE, REVOKED }
    public enum RevocationStatus { PENDING, APPROVED }

    public record User(long id, String fullName, String email, String passwordHash, Role role, boolean active) {}
    public record Session(String idHash, long userId, Instant expiresAt, boolean revoked) {}
    public record Credential(long id, long graduateId, long issuedBy, String programme,
                             LocalDate awardDate, CredentialStatus status, Instant issuedAt) {}
    public record Revocation(long id, long credentialId, long requestedBy, String reason,
                             RevocationStatus status, Instant createdAt, Long approvedBy) {}
    public record ShareLink(String tokenHash, long credentialId, Instant expiresAt) {}
    public record AuditEvent(long id, Long actorId, String event, String entityType,
                             Long entityId, Instant occurredAt, String detail) {}

    public interface UserRepository {
        Optional<User> findUser(long id);
        Optional<User> findByEmail(String email);
        List<User> users();
    }

    public interface SessionRepository {
        void saveSession(Session session);
        Optional<Session> findSession(String idHash);
        void revokeSession(String idHash);
    }

    public interface CredentialRepository {
        Credential saveCredential(Credential credential);
        Optional<Credential> findCredential(long id);
        List<Credential> credentials();
    }

    public interface RevocationRepository {
        Revocation saveRevocation(Revocation revocation);
        Revocation savePendingIfNone(Revocation revocation);
        Optional<Revocation> findRevocation(long id);
        List<Revocation> revocations();
        Revocation approveAtomically(long id, long approverId, Instant approvedAt);
    }

    public interface ShareLinkRepository {
        void saveShareLink(ShareLink shareLink);
        Optional<ShareLink> findShareLink(String tokenHash);
    }

    public interface AuditRepository {
        AuditEvent saveAudit(AuditEvent auditEvent);
        List<AuditEvent> auditEvents();
    }
}
