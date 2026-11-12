package zm.ac.mu.registry.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import zm.ac.mu.registry.data.Data.*;

@Repository
@Profile("mem")
public class InMemoryData implements UserRepository, SessionRepository, CredentialRepository,
        RevocationRepository, ShareLinkRepository, AuditRepository {
    private final Map<Long, User> users = new LinkedHashMap<>();
    private final Map<String, Session> sessions = new LinkedHashMap<>();
    private final Map<Long, Credential> credentials = new LinkedHashMap<>();
    private final Map<Long, Revocation> revocations = new LinkedHashMap<>();
    private final Map<String, ShareLink> shareLinks = new LinkedHashMap<>();
    private final List<AuditEvent> auditEvents = new ArrayList<>();
    private long nextCredentialId = 1;
    private long nextRevocationId = 1;
    private long nextAuditId = 1;

    public InMemoryData() {
        String hash = new BCryptPasswordEncoder().encode("Password123!");
        addUser(1, "Registrar One", "reg1@mu.ac.zm", hash, Role.REGISTRAR);
        addUser(2, "Registrar Two", "reg2@mu.ac.zm", hash, Role.REGISTRAR);
        addUser(3, "Graduate One", "grad@mu.ac.zm", hash, Role.GRADUATE);
        addUser(4, "Graduate Two", "grad2@mu.ac.zm", hash, Role.GRADUATE);
        addUser(5, "Registry Auditor", "aud@mu.ac.zm", hash, Role.AUDITOR);
    }

    private void addUser(long id, String name, String email, String hash, Role role) {
        users.put(id, new User(id, name, email, hash, role, true));
    }

    @Override public synchronized Optional<User> findUser(long id) { return Optional.ofNullable(users.get(id)); }
    @Override public synchronized Optional<User> findByEmail(String email) {
        return users.values().stream().filter(user -> user.email().equalsIgnoreCase(email)).findFirst();
    }
    @Override public synchronized List<User> users() { return List.copyOf(users.values()); }
    @Override public synchronized void saveSession(Session session) { sessions.put(session.idHash(), session); }
    @Override public synchronized Optional<Session> findSession(String idHash) { return Optional.ofNullable(sessions.get(idHash)); }
    @Override public synchronized void revokeSession(String idHash) {
        Session session = sessions.get(idHash);
        if (session != null) sessions.put(idHash, new Session(session.idHash(), session.userId(), session.expiresAt(), true));
    }

    @Override public synchronized Credential saveCredential(Credential credential) {
        Credential saved = credential.id() == 0
                ? new Credential(nextCredentialId++, credential.graduateId(), credential.issuedBy(),
                    credential.programme(), credential.awardDate(), credential.status(), credential.issuedAt())
                : credential;
        credentials.put(saved.id(), saved);
        return saved;
    }
    @Override public synchronized Optional<Credential> findCredential(long id) { return Optional.ofNullable(credentials.get(id)); }
    @Override public synchronized List<Credential> credentials() { return List.copyOf(credentials.values()); }

    @Override public synchronized Revocation saveRevocation(Revocation revocation) {
        Revocation saved = revocation.id() == 0
                ? new Revocation(nextRevocationId++, revocation.credentialId(), revocation.requestedBy(),
                    revocation.reason(), revocation.status(), revocation.createdAt(), revocation.approvedBy())
                : revocation;
        revocations.put(saved.id(), saved);
        return saved;
    }
    @Override public synchronized Revocation savePendingIfNone(Revocation revocation) {
        Credential credential = credentials.get(revocation.credentialId());
        if (credential == null || credential.status() != CredentialStatus.ACTIVE
                || revocations.values().stream().anyMatch(existing ->
                        existing.credentialId() == revocation.credentialId()
                                && existing.status() == RevocationStatus.PENDING)) return null;
        return saveRevocation(revocation);
    }
    @Override public synchronized Optional<Revocation> findRevocation(long id) { return Optional.ofNullable(revocations.get(id)); }
    @Override public synchronized List<Revocation> revocations() { return List.copyOf(revocations.values()); }

    @Override
    public synchronized Revocation approveAtomically(long id, long approverId, Instant approvedAt) {
        Revocation current = revocations.get(id);
        if (current == null) return null;
        if (current.status() != RevocationStatus.PENDING) throw new IllegalStateException("REVOCATION_NOT_PENDING");
        Credential credential = credentials.get(current.credentialId());
        if (credential == null) return null;
        if (credential.status() != CredentialStatus.ACTIVE) throw new IllegalStateException("CREDENTIAL_NOT_ACTIVE");
        Revocation approved = new Revocation(current.id(), current.credentialId(), current.requestedBy(),
                current.reason(), RevocationStatus.APPROVED, approvedAt, approverId);
        Credential revoked = new Credential(credential.id(), credential.graduateId(), credential.issuedBy(),
                credential.programme(), credential.awardDate(), CredentialStatus.REVOKED, credential.issuedAt());
        revocations.put(id, approved);
        credentials.put(credential.id(), revoked);
        return approved;
    }

    @Override public synchronized void saveShareLink(ShareLink shareLink) { shareLinks.put(shareLink.tokenHash(), shareLink); }
    @Override public synchronized Optional<ShareLink> findShareLink(String tokenHash) { return Optional.ofNullable(shareLinks.get(tokenHash)); }

    @Override public synchronized AuditEvent saveAudit(AuditEvent event) {
        AuditEvent saved = new AuditEvent(nextAuditId++, event.actorId(), event.event(), event.entityType(),
                event.entityId(), event.occurredAt(), event.detail());
        auditEvents.add(saved);
        return saved;
    }
    @Override public synchronized List<AuditEvent> auditEvents() {
        return auditEvents.stream().sorted(Comparator.comparing(AuditEvent::occurredAt).reversed()
                .thenComparing(AuditEvent::id, Comparator.reverseOrder())).toList();
    }
}
