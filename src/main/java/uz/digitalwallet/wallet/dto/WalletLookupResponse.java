package uz.digitalwallet.wallet.dto;

import uz.digitalwallet.common.money.Currency;

public record WalletLookupResponse(String number, Currency currency, String ownerName) {
}
