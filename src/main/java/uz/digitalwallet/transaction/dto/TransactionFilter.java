package uz.digitalwallet.transaction.dto;

import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;
import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.transaction.TransactionStatus;
import uz.digitalwallet.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Query parameters for transaction search. Dates are calendar days in the business time zone, both inclusive.
 */
public record TransactionFilter(
        UUID walletId,
        TransactionType type,
        TransactionStatus status,
        Currency currency,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @PositiveOrZero BigDecimal minAmount,
        @PositiveOrZero BigDecimal maxAmount) {
}
