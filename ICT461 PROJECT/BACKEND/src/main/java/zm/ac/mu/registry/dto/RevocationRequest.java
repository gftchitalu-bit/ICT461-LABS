package zm.ac.mu.registry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RevocationRequest(
        @NotBlank @Size(min = 3, max = 500) @Pattern(regexp = "^[^<>]+$") String reason) {}
