package uz.digitalwallet.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.digitalwallet.admin.dto.UpdateWalletStatusRequest;
import uz.digitalwallet.wallet.WalletService;
import uz.digitalwallet.wallet.dto.WalletResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/wallets")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminWalletController {

    private final WalletService walletService;

    @PatchMapping("/{walletId}/status")
    @Operation(summary = "Freeze or unfreeze a wallet")
    public WalletResponse changeStatus(@PathVariable UUID walletId,
                                       @Valid @RequestBody UpdateWalletStatusRequest request) {
        return walletService.changeStatus(walletId, request.status());
    }
}
