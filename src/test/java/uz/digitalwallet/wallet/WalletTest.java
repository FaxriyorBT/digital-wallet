package uz.digitalwallet.wallet;

import org.junit.jupiter.api.Test;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.money.Currency;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uz.digitalwallet.support.TestData.user;
import static uz.digitalwallet.support.TestData.wallet;

class WalletTest {

    @Test
    void creditAndDebitReturnNewBalance() {
        Wallet wallet = wallet(user("Ali Valiyev"), Currency.UZS, "0");

        assertThat(wallet.credit(new BigDecimal("1000.00"))).isEqualByComparingTo("1000.00");
        assertThat(wallet.debit(new BigDecimal("250.50"))).isEqualByComparingTo("749.50");
    }

    @Test
    void cannotGoBelowZero() {
        Wallet wallet = wallet(user("Ali Valiyev"), Currency.UZS, "100");

        assertThatThrownBy(() -> wallet.debit(new BigDecimal("100.01")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(wallet.getBalance()).isEqualByComparingTo("100");
    }

    @Test
    void closesOnlyEmptyWallet() {
        Wallet wallet = wallet(user("Ali Valiyev"), Currency.USD, "5");

        assertThatThrownBy(wallet::close)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.WALLET_NOT_EMPTY);

        wallet.debit(new BigDecimal("5"));
        wallet.close();
        assertThat(wallet.getStatus()).isEqualTo(WalletStatus.CLOSED);
    }

    @Test
    void closedWalletStatusCannotChange() {
        Wallet wallet = wallet(user("Ali Valiyev"), Currency.EUR, "0");
        wallet.close();

        assertThatThrownBy(() -> wallet.changeStatus(WalletStatus.ACTIVE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STATUS_CHANGE_NOT_ALLOWED);
    }

    @Test
    void masksOwnerNameForLookup() {
        assertThat(WalletService.maskName("Aziz Karimov")).isEqualTo("Aziz K.");
        assertThat(WalletService.maskName("  Ali   Valiyev  ")).isEqualTo("Ali V.");
        assertThat(WalletService.maskName("Madina")).isEqualTo("Madina");
    }
}
