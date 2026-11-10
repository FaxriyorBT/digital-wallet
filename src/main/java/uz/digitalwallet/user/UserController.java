package uz.digitalwallet.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.digitalwallet.common.security.CurrentUserId;
import uz.digitalwallet.user.dto.ChangePasswordRequest;
import uz.digitalwallet.user.dto.UpdateProfileRequest;
import uz.digitalwallet.user.dto.UserResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "Profile")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get the current user's profile")
    public UserResponse getProfile(@CurrentUserId UUID userId) {
        return userService.getProfile(userId);
    }

    @PatchMapping
    @Operation(summary = "Update name and/or phone number")
    public UserResponse updateProfile(@CurrentUserId UUID userId, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId, request);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change password; all refresh tokens of the user are revoked")
    public void changePassword(@CurrentUserId UUID userId, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
    }
}
