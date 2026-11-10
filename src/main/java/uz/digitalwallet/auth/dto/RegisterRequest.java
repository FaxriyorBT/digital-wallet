package uz.digitalwallet.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import uz.digitalwallet.common.validation.PhoneNumber;
import uz.digitalwallet.common.validation.StrongPassword;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @PhoneNumber String phone,
        @StrongPassword String password,
        @NotBlank @Size(min = 2, max = 100) String fullName) {
}
