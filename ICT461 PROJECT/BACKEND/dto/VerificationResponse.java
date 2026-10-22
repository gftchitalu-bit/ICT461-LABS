package zm.ac.mu.registry.dto;

import java.time.LocalDate;

public record VerificationRespose(
    boolean valid,
    String status,
    String programme,
    LocalDate awardDate) {}