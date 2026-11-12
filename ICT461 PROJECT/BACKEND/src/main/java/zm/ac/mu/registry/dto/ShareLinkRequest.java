package zm.ac.mu.registry.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ShareLinkRequest(@Min(1) @Max(720) int expiresInHours) {}
