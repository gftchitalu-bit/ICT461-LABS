package zm.ac.mu.registry.dto;

import java.time.Localdate;

public record CredentialResponse(
    String id,
    String graduateId,
    String programme,
    LocalDate awardDate,
    String status) {}