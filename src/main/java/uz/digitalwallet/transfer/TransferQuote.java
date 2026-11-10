package uz.digitalwallet.transfer;

import uz.digitalwallet.common.money.Currency;

import java.math.BigDecimal;

/**
 * @param amount          what the sender wants to send, in the source currency
 * @param fee             charged on top of the amount, in the source currency
 * @param totalDebit      amount + fee, this is what leaves the source wallet
 * @param creditedAmount  what the recipient gets, in the target currency
 * @param exchangeRate    source-to-target rate, null when no conversion is needed
 */
public record TransferQuote(
        BigDecimal amount,
        Currency currency,
        BigDecimal fee,
        BigDecimal totalDebit,
        BigDecimal creditedAmount,
        Currency creditedCurrency,
        BigDecimal exchangeRate) {

    static TransferQuote of(BigDecimal amount, Currency currency, BigDecimal fee, BigDecimal creditedAmount,
                            Currency creditedCurrency, BigDecimal exchangeRate) {
        return new TransferQuote(amount, currency, fee, amount.add(fee), creditedAmount, creditedCurrency, exchangeRate);
    }

    boolean isCrossCurrency() {
        return currency != creditedCurrency;
    }

    boolean hasFee() {
        return fee.signum() > 0;
    }
}
