package uz.digitalwallet.transaction.dto;

import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.transaction.TransactionStatus;
import uz.digitalwallet.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        TransactionStatus status,
        String declineReason,
        String sourceWalletNumber,
        String targetWalletNumber,
        BigDecimal amount,
        Currency currency,
        BigDecimal fee,
        BigDecimal creditedAmount,
        Currency creditedCurrency,
        BigDecimal exchangeRate,
        String description,
        Instant createdAt) {
}
