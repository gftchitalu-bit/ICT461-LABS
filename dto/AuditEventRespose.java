package zm.ac.mu.registry.dto;

import java.time.Instant;

public record AuditEventRespose(
    String id,
    String action,
    String actor,
    Instant timestamp) {}