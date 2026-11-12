package zm.ac.mu.registry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevocationRequest(
    @NotBlank @Size(max = 500) String reason) {}