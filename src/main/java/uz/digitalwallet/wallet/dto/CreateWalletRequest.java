package uz.digitalwallet.wallet.dto;

import jakarta.validation.constraints.NotNull;
import uz.digitalwallet.common.money.Currency;

public record CreateWalletRequest(@NotNull Currency currency) {
}
