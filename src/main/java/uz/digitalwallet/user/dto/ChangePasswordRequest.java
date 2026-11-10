package uz.digitalwallet.user.dto;

import jakarta.validation.constraints.NotBlank;
import uz.digitalwallet.common.validation.StrongPassword;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @StrongPassword String newPassword) {
}
