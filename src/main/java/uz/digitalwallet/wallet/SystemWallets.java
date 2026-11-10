package uz.digitalwallet.wallet;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.digitalwallet.common.money.Currency;

@Component
@RequiredArgsConstructor
public class SystemWallets {

    private final WalletRepository wallets;

    public Wallet funding(Currency currency) {
        return find(WalletType.FUNDING, currency);
    }

    public Wallet fee(Currency currency) {
        return find(WalletType.FEE, currency);
    }

    public Wallet exchange(Currency currency) {
        return find(WalletType.EXCHANGE, currency);
    }

    private Wallet find(WalletType type, Currency currency) {
        return wallets.findByTypeAndCurrency(type, currency)
                .orElseThrow(() -> new IllegalStateException("System wallet " + type + " " + currency + " is missing"));
    }
}
