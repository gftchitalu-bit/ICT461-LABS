package zm.ac.mu.registry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LogiRequest(
    @NotBlank @Email String email,
    @NotBlank String password) {}