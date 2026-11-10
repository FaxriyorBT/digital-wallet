package uz.digitalwallet.wallet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.LuhnCheck;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uz.digitalwallet.common.security.CurrentUserId;
import uz.digitalwallet.wallet.dto.CreateWalletRequest;
import uz.digitalwallet.wallet.dto.WalletLookupResponse;
import uz.digitalwallet.wallet.dto.WalletResponse;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Open a wallet; a user can have one open wallet per currency")
    public ResponseEntity<WalletResponse> open(@CurrentUserId UUID userId,
                                               @Valid @RequestBody CreateWalletRequest request) {
        WalletResponse wallet = walletService.open(userId, request.currency());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(wallet.id())
                .toUri();
        return ResponseEntity.created(location).body(wallet);
    }

    @GetMapping
    @Operation(summary = "List the current user's wallets")
    public List<WalletResponse> findAll(@CurrentUserId UUID userId) {
        return walletService.findAll(userId);
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Get a wallet with its current balance")
    public WalletResponse get(@CurrentUserId UUID userId, @PathVariable UUID walletId) {
        return walletService.get(userId, walletId);
    }

    @PostMapping("/{walletId}/close")
    @Operation(summary = "Close a wallet; the balance must be zero")
    public WalletResponse close(@CurrentUserId UUID userId, @PathVariable UUID walletId) {
        return walletService.close(userId, walletId);
    }

    @GetMapping("/lookup")
    @Operation(summary = "Check a recipient before sending money; the owner's name is partially hidden")
    public WalletLookupResponse lookup(
            @RequestParam @Pattern(regexp = "\\d{16}", message = "must contain exactly 16 digits") @LuhnCheck
            String number) {
        return walletService.lookup(number);
    }
}
