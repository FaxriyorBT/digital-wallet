package uz.digitalwallet.ledger;

import uz.digitalwallet.wallet.Wallet;

import java.math.BigDecimal;

public record Posting(Wallet wallet, EntryDirection direction, BigDecimal amount) {

    public static Posting debit(Wallet wallet, BigDecimal amount) {
        return new Posting(wallet, EntryDirection.DEBIT, amount);
    }

    public static Posting credit(Wallet wallet, BigDecimal amount) {
        return new Posting(wallet, EntryDirection.CREDIT, amount);
    }
}
