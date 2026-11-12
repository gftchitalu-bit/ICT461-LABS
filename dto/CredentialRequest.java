package zm.am.mu.registry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CredentialRequest(
    @NotBlank String graduateId,
    @NotBlank @Size(max=120) String programme,
    @NotNull @PastOrPresent LocalDate awardDate) {}