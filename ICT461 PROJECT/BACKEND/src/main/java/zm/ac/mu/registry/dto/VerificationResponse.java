package zm.ac.mu.registry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerificationResponse(boolean valid, String status, String programme, LocalDate awardDate) {
    public static VerificationResponse unavailable(String status) {
        return new VerificationResponse(false, status, null, null);
    }
    public static VerificationResponse active(String programme, LocalDate awardDate) {
        return new VerificationResponse(true, "ACTIVE", programme, awardDate);
    }
}
