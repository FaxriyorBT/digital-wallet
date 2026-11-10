package uz.digitalwallet.admin.dto;

import jakarta.validation.constraints.NotNull;
import uz.digitalwallet.user.UserStatus;

public record UpdateUserStatusRequest(@NotNull UserStatus status) {
}
