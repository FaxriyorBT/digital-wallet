package uz.digitalwallet.transfer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uz.digitalwallet.common.security.CurrentUserId;
import uz.digitalwallet.transaction.dto.TransactionResponse;
import uz.digitalwallet.transfer.dto.TopUpRequest;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets/{walletId}/top-ups")
@RequiredArgsConstructor
@Tag(name = "Wallets")
public class TopUpController {

    private final TopUpService topUpService;

    @PostMapping
    @Operation(summary = "Top up a wallet (simulated card payment)")
    public ResponseEntity<TransactionResponse> topUp(
            @CurrentUserId UUID userId,
            @PathVariable UUID walletId,
            @RequestHeader("Idempotency-Key") @Size(min = 8, max = 64) String idempotencyKey,
            @Valid @RequestBody TopUpRequest request) {
        TransactionResponse transaction = topUpService.topUp(userId, walletId, idempotencyKey, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/transactions/{id}")
                .buildAndExpand(transaction.id())
                .toUri();
        return ResponseEntity.created(location).body(transaction);
    }
}
