package zm.ac.mu.registry.dto;

import java.time.Instant;

public record ShareLinkResponse(
    String token,
    String url,
    String qrCode,
    Instant expiresAt) {}