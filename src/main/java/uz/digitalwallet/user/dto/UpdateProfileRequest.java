package uz.digitalwallet.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uz.digitalwallet.common.validation.PhoneNumber;

/**
 * Partial update: fields left out (null) are not changed.
 */
public record UpdateProfileRequest(
        @Size(min = 2, max = 100) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String fullName,
        @PhoneNumber String phone) {
}
