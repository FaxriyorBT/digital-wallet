package uz.digitalwallet.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.digitalwallet.admin.dto.UpdateUserStatusRequest;
import uz.digitalwallet.common.web.PageResponse;
import uz.digitalwallet.user.UserService;
import uz.digitalwallet.user.UserStatus;
import uz.digitalwallet.user.dto.UserResponse;
import uz.digitalwallet.wallet.WalletService;
import uz.digitalwallet.wallet.dto.WalletResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminUserController {

    private final UserService userService;
    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Search users by email, name or phone")
    public PageResponse<UserResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UserStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return userService.search(query, status, pageable);
    }

    @GetMapping("/{userId}/wallets")
    @Operation(summary = "Wallets of a user")
    public List<WalletResponse> wallets(@PathVariable UUID userId) {
        return walletService.findAll(userId);
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Block or unblock a user; blocking also closes all of the user's sessions")
    public UserResponse changeStatus(@PathVariable UUID userId, @Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.changeStatus(userId, request.status());
    }
}
