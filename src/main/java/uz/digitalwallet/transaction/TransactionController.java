package uz.digitalwallet.transaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.digitalwallet.common.security.CurrentUserId;
import uz.digitalwallet.common.web.PageResponse;
import uz.digitalwallet.transaction.dto.TransactionFilter;
import uz.digitalwallet.transaction.dto.TransactionResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions")
public class TransactionController {

    private final TransactionQueryService queryService;

    @GetMapping
    @Operation(summary = "Transaction history of the current user; all filters are optional")
    public PageResponse<TransactionResponse> findAll(
            @CurrentUserId UUID userId,
            @Valid @ParameterObject TransactionFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return queryService.findForUser(userId, filter, pageable);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get one transaction")
    public TransactionResponse get(@CurrentUserId UUID userId, @PathVariable UUID transactionId) {
        return queryService.getForUser(userId, transactionId);
    }
}
