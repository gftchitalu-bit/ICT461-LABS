package zm.ac.mu.registry.service;

import zm.ac.mu.registry.dto.*;

public interface ShareLinkService {
    ShareLinkResponse create(String credentialId, ShareLinkRequest request);
}