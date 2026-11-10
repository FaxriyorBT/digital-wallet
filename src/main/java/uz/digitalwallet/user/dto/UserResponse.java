package uz.digitalwallet.user.dto;

import uz.digitalwallet.user.Role;
import uz.digitalwallet.user.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String phone,
        String fullName,
        Role role,
        UserStatus status,
        Instant createdAt) {
}
