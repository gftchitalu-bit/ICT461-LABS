package zm.ac.mu.registry.dto;

public record RevocationRespose(
       String revocationId,
       String credentialId,
       String status) {}