package uz.digitalwallet.ledger.dto;

import uz.digitalwallet.ledger.EntryDirection;
import uz.digitalwallet.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StatementEntryResponse(
        UUID transactionId,
        TransactionType transactionType,
        EntryDirection direction,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String counterpartyWalletNumber,
        String description,
        Instant createdAt) {
}
