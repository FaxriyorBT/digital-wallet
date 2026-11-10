package uz.digitalwallet.support;

import org.springframework.test.util.ReflectionTestUtils;
import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.user.Role;
import uz.digitalwallet.user.User;
import uz.digitalwallet.wallet.Wallet;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builders for unit tests that work with entities without a database.
 */
public final class TestData {

    private TestData() {
    }

    public static User user(String fullName) {
        User user = User.create(UUID.randomUUID() + "@test.uz", randomPhone(), "hash", fullName, Role.USER);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    public static Wallet wallet(User owner, Currency currency, String balance) {
        Wallet wallet = Wallet.open(owner, currency, "4000000000000002");
        ReflectionTestUtils.setField(wallet, "id", UUID.randomUUID());
        if (new BigDecimal(balance).signum() > 0) {
            wallet.credit(new BigDecimal(balance));
        }
        return wallet;
    }

    public static String randomPhone() {
        return "+99890" + ThreadLocalRandom.current().nextInt(1_000_000, 10_000_000);
    }
}
