package uz.digitalwallet.wallet;

public enum WalletType {
    PERSONAL,
    /** Counterpart of top-ups: money that came into the system from outside. */
    FUNDING,
    /** Collected transfer fees. */
    FEE,
    /** Currency position used when a transfer converts between currencies. */
    EXCHANGE
}
