pakage zm.ac.mu.registry.servce;

import zm.ac.mu.registry.dto.*;

public interface CredentialService {
    CredentialResponse issue(CredentialRequest request);
    CredentialResponse get(String id);
    pageResponse<CredentialResponse> search(String q, String status, int page, int size);
}