package uz.digitalwallet.transaction;

import uz.digitalwallet.common.money.Currency;

import java.math.BigDecimal;

public record CurrencyTurnover(
        TransactionType type,
        Currency currency,
        long count,
        BigDecimal volume,
        BigDecimal fees) {
}
