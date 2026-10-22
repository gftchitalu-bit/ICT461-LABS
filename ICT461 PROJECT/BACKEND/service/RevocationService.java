package zm.ac.mu.registry.service;

import zm.ac.mu.registry.dto.*;

public interface RevocationService {
    RevocationResponse request(String credentialId, RevocationRequest request);
    RevocationResponse approve(String revocationId);
}