package uz.digitalwallet.admin.dto;

import jakarta.validation.constraints.NotNull;
import uz.digitalwallet.wallet.WalletStatus;

public record UpdateWalletStatusRequest(@NotNull WalletStatus status) {
}
