package uz.digitalwallet.wallet.dto;

import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.wallet.WalletStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        String number,
        Currency currency,
        BigDecimal balance,
        WalletStatus status,
        Instant createdAt) {
}
