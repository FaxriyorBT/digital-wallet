package uz.digitalwallet.transfer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uz.digitalwallet.common.security.CurrentUserId;
import uz.digitalwallet.transaction.dto.TransactionResponse;
import uz.digitalwallet.transfer.dto.TransferRequest;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers")
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/quote")
    @Operation(summary = "Calculate the fee and the converted amount without moving any money")
    public TransferQuote quote(@CurrentUserId UUID userId, @Valid @RequestBody TransferRequest request) {
        return transferService.quote(userId, request);
    }

    @PostMapping
    @Operation(summary = "Send money to another wallet",
            description = "Repeating the request with the same Idempotency-Key returns the first result "
                    + "instead of sending the money twice.")
    public ResponseEntity<TransactionResponse> transfer(
            @CurrentUserId UUID userId,
            @RequestHeader("Idempotency-Key") @Size(min = 8, max = 64) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse transaction = transferService.transfer(userId, idempotencyKey, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/transactions/{id}")
                .buildAndExpand(transaction.id())
                .toUri();
        return ResponseEntity.created(location).body(transaction);
    }
}
