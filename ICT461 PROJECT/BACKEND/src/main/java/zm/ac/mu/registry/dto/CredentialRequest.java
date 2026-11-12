package zm.ac.mu.registry.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CredentialRequest(
        @Min(1) long graduateId,
        @NotBlank @Size(min = 3, max = 120) @Pattern(regexp = "^[A-Za-z0-9 .,&()'/-]+$")
        String programme,
        @NotNull @PastOrPresent LocalDate awardDate) {}
