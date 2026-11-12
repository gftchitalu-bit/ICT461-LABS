package zm.ac.mu.registry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(min = 5, max = 190) String email,
        @NotNull @Size(min = 8, max = 72) String password) {}
