package zm.ac.mu.registry.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import zm.ac.mu.registry.data.Data.Credential;
import zm.ac.mu.registry.data.Data.CredentialRepository;
import zm.ac.mu.registry.data.Data.CredentialStatus;
import zm.ac.mu.registry.data.Data.Role;
import zm.ac.mu.registry.data.Data.User;
import zm.ac.mu.registry.data.Data.UserRepository;
import zm.ac.mu.registry.dto.CredentialRequest;
import zm.ac.mu.registry.dto.CredentialResponse;
import zm.ac.mu.registry.dto.PageResponse;
import zm.ac.mu.registry.web.ApiFailure;

@Service
public class CredentialService {
    private final CredentialRepository credentials;
    private final UserRepository users;
    private final AuditService audit;

    public CredentialService(CredentialRepository credentials, UserRepository users, AuditService audit) {
        this.credentials = credentials;
        this.users = users;
        this.audit = audit;
    }

    public CredentialResponse issue(CredentialRequest request, User registrar) {
        User graduate = users.findUser(request.graduateId())
                .filter(user -> user.role() == Role.GRADUATE)
                .orElseThrow(() -> new ApiFailure(HttpStatus.UNPROCESSABLE_ENTITY,
                        "graduateId must identify an existing graduate.", "graduateId"));
        Credential saved = credentials.saveCredential(new Credential(0, graduate.id(), registrar.id(),
                request.programme(), request.awardDate(), CredentialStatus.ACTIVE, java.time.Instant.now()));
        audit.record(registrar.id(), "CREDENTIAL_ISSUED", "CREDENTIAL", saved.id(),
                "Issued to graduate " + graduate.id());
        return CredentialResponse.from(saved);
    }

    public PageResponse<CredentialResponse> search(String q, String status, int page, int size) {
        String query = q == null ? null : q.toLowerCase(Locale.ROOT);
        List<Credential> filtered = credentials.credentials().stream()
                .filter(credential -> status == null || credential.status().name().equals(status))
                .filter(credential -> matches(credential, query))
                .sorted(Comparator.comparingLong(Credential::id).reversed()).toList();
        int from = Math.min(filtered.size(), page * size);
        int to = Math.min(filtered.size(), from + size);
        return new PageResponse<>(filtered.subList(from, to).stream().map(CredentialResponse::from).toList(),
                page, size, filtered.size(), (int) Math.ceil((double) filtered.size() / size));
    }

    private boolean matches(Credential credential, String query) {
        if (query == null) return true;
        User graduate = users.findUser(credential.graduateId()).orElse(null);
        return credential.programme().toLowerCase(Locale.ROOT).contains(query)
                || (graduate != null && (graduate.fullName().toLowerCase(Locale.ROOT).contains(query)
                || graduate.email().toLowerCase(Locale.ROOT).contains(query)));
    }

    public CredentialResponse get(long id, User requester) {
        Credential credential = credentials.findCredential(id)
                .orElseThrow(() -> new ApiFailure(HttpStatus.NOT_FOUND, "Credential not found."));
        if (requester.role() == Role.GRADUATE && credential.graduateId() != requester.id())
            throw new ApiFailure(HttpStatus.FORBIDDEN, "You may only access your own credentials.");
        return CredentialResponse.from(credential);
    }

    public Credential require(long id) {
        return credentials.findCredential(id)
                .orElseThrow(() -> new ApiFailure(HttpStatus.NOT_FOUND, "Credential not found."));
    }

}
