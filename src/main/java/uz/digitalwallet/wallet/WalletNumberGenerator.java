package uz.digitalwallet.wallet;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 16-digit wallet numbers with a Luhn check digit (the same scheme bank cards use), so a mistyped
 * number is rejected by validation before it ever reaches the database.
 */
@Component
@RequiredArgsConstructor
public class WalletNumberGenerator {

    private static final int LENGTH = 16;

    private final SecureRandom random = new SecureRandom();
    private final WalletRepository wallets;

    public String generate() {
        String number;
        do {
            number = randomNumber();
        } while (wallets.existsByNumber(number));
        return number;
    }

    String randomNumber() {
        StringBuilder digits = new StringBuilder(LENGTH);
        // personal wallets never start with 0, that range is used by system wallets
        digits.append(1 + random.nextInt(9));
        while (digits.length() < LENGTH - 1) {
            digits.append(random.nextInt(10));
        }
        return digits.append(luhnCheckDigit(digits)).toString();
    }

    static int luhnCheckDigit(CharSequence payload) {
        int sum = 0;
        for (int i = 0; i < payload.length(); i++) {
            int digit = payload.charAt(payload.length() - 1 - i) - '0';
            if (i % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        return (10 - sum % 10) % 10;
    }
}
