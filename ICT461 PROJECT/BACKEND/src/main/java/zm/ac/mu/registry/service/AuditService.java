package zm.ac.mu.registry.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import zm.ac.mu.registry.data.Data.AuditEvent;
import zm.ac.mu.registry.data.Data.AuditRepository;
import zm.ac.mu.registry.dto.AuditEventResponse;
import zm.ac.mu.registry.dto.PageResponse;

@Service
public class AuditService {
    private final AuditRepository audit;

    public AuditService(AuditRepository audit) { this.audit = audit; }

    public void record(Long actorId, String event, String entityType, Long entityId, String detail) {
        audit.saveAudit(new AuditEvent(0, actorId, event, entityType, entityId, Instant.now(), detail));
    }

    public PageResponse<AuditEventResponse> page(int page, int size) {
        List<AuditEvent> all = audit.auditEvents();
        int from = Math.min(all.size(), page * size);
        int to = Math.min(all.size(), from + size);
        return new PageResponse<>(all.subList(from, to).stream().map(AuditEventResponse::from).toList(),
                page, size, all.size(), (int) Math.ceil((double) all.size() / size));
    }
}
