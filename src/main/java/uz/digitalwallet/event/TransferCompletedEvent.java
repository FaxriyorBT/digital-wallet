package uz.digitalwallet.event;

import uz.digitalwallet.common.money.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Carries everything the notification side needs, so consumers do not have to call back into the wallet module.
 */
public record TransferCompletedEvent(
        UUID eventId,
        UUID transactionId,
        Participant sender,
        Participant recipient,
        BigDecimal amount,
        Currency currency,
        BigDecimal fee,
        BigDecimal creditedAmount,
        Currency creditedCurrency,
        Instant occurredAt) {

    public record Participant(String email, String fullName, String walletNumber) {
    }
}
