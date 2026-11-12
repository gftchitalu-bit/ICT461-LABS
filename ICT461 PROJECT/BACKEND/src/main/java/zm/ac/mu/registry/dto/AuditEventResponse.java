package zm.ac.mu.registry.dto;

import java.time.Instant;
import zm.ac.mu.registry.data.Data.AuditEvent;

public record AuditEventResponse(long id, Long actorId, String event, String entityType,
                                 Long entityId, Instant occurredAt, String detail) {
    public static AuditEventResponse from(AuditEvent audit) {
        return new AuditEventResponse(audit.id(), audit.actorId(), audit.event(), audit.entityType(),
                audit.entityId(), audit.occurredAt(), audit.detail());
    }
}
