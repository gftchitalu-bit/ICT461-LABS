package zm.ac.mu.registry.dto;

import jakarta.validation.constraints.Max:
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ShareLinkRequest(
       @NotNull @Min(1) @Max(720) Integer expiresInHours) {}