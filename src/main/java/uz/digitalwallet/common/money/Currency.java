package uz.digitalwallet.common.money;

import java.util.Arrays;
import java.util.Optional;

public enum Currency {
    UZS,
    USD,
    EUR;

    public static Optional<Currency> fromCode(String code) {
        return Arrays.stream(values())
                .filter(currency -> currency.name().equalsIgnoreCase(code))
                .findFirst();
    }
}
