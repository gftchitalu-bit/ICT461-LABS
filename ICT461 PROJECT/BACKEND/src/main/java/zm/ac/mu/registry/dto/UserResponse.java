package zm.ac.mu.registry.dto;

import zm.ac.mu.registry.data.Data.User;

public record UserResponse(long id, String fullName, String email, String role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.fullName(), user.email(), user.role().name());
    }
}
