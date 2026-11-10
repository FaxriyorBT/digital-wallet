package uz.digitalwallet.wallet;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WalletNumberGeneratorTest {

    private final WalletNumberGenerator generator = new WalletNumberGenerator(mock(WalletRepository.class));

    @Test
    void calculatesKnownLuhnCheckDigit() {
        // classic example from the Luhn algorithm description: 7992739871 -> check digit 3
        assertThat(WalletNumberGenerator.luhnCheckDigit("7992739871")).isEqualTo(3);
    }

    @RepeatedTest(50)
    void generatesValidSixteenDigitNumbers() {
        String number = generator.randomNumber();

        assertThat(number).hasSize(16).containsOnlyDigits().doesNotStartWith("0");
        assertThat(isValidLuhn(number)).isTrue();
    }

    private static boolean isValidLuhn(String number) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (doubleDigit) {
                digit = digit * 2 > 9 ? digit * 2 - 9 : digit * 2;
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
