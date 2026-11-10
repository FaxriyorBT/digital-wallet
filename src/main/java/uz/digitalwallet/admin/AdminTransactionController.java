package uz.digitalwallet.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.digitalwallet.admin.dto.DailyStatsResponse;
import uz.digitalwallet.common.web.PageResponse;
import uz.digitalwallet.transaction.TransactionQueryService;
import uz.digitalwallet.transaction.dto.TransactionFilter;
import uz.digitalwallet.transaction.dto.TransactionResponse;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminTransactionController {

    private final TransactionQueryService queryService;
    private final StatsService statsService;

    @GetMapping("/transactions")
    @Operation(summary = "Search all transactions; all filters are optional")
    public PageResponse<TransactionResponse> search(
            @Valid @ParameterObject TransactionFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return queryService.search(filter, pageable);
    }

    @GetMapping("/stats/daily")
    @Operation(summary = "Turnover and collected fees per currency for one day (today by default)")
    public DailyStatsResponse dailyStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return statsService.daily(date);
    }
}
