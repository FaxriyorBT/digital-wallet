package uz.digitalwallet.ledger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.digitalwallet.common.security.CurrentUserId;
import uz.digitalwallet.common.web.PageResponse;
import uz.digitalwallet.ledger.dto.StatementEntryResponse;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Wallets")
public class StatementController {

    private final StatementService statementService;

    @GetMapping("/api/v1/wallets/{walletId}/statement")
    @Operation(summary = "Every balance change of the wallet with the balance after it; last 30 days by default")
    public PageResponse<StatementEntryResponse> getStatement(
            @CurrentUserId UUID userId,
            @PathVariable UUID walletId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return statementService.getStatement(userId, walletId, from, to, pageable);
    }
}
