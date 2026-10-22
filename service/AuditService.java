package zm.ac.mu.registry.service;

import zm.ac.mu.registry.dto.*;

public interface AuditService {
    PageRespose<AuditEventResponse> events(int page, int size);
}