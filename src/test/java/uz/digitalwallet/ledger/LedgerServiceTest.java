package uz.digitalwallet.ledger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.transaction.Transaction;
import uz.digitalwallet.user.User;
import uz.digitalwallet.wallet.Wallet;
import uz.digitalwallet.wallet.WalletType;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uz.digitalwallet.support.TestData.user;
import static uz.digitalwallet.support.TestData.wallet;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository entries;

    @InjectMocks
    private LedgerService ledgerService;

    private final Transaction transaction = mock(Transaction.class);

    @Test
    @SuppressWarnings("unchecked")
    void appliesBalancedPostingsToPersonalWallets() {
        User alice = user("Alice Smith");
        Wallet source = wallet(alice, Currency.UZS, "1000");
        Wallet target = wallet(user("Bob Brown"), Currency.UZS, "0");
        Wallet fee = systemWallet(Currency.UZS);

        ledgerService.record(transaction, List.of(
                Posting.debit(source, new BigDecimal("105")),
                Posting.credit(target, new BigDecimal("100")),
                Posting.credit(fee, new BigDecimal("5"))));

        assertThat(source.getBalance()).isEqualByComparingTo("895");
        assertThat(target.getBalance()).isEqualByComparingTo("100");

        ArgumentCaptor<List<LedgerEntry>> saved = ArgumentCaptor.forClass(List.class);
        verify(entries).saveAll(saved.capture());
        // compareTo instead of equals: 895.00 and 895 are equal amounts with a different scale
        assertThat(saved.getValue())
                .extracting(LedgerEntry::getBalanceAfter)
                .usingElementComparator(Comparator.nullsFirst(Comparator.<BigDecimal>naturalOrder()))
                .containsExactly(new BigDecimal("895"), new BigDecimal("100"), null);
    }

    @Test
    void rejectsPostingsThatDoNotCancelOut() {
        Wallet source = wallet(user("Alice Smith"), Currency.UZS, "1000");
        Wallet target = wallet(user("Bob Brown"), Currency.UZS, "0");

        assertThatThrownBy(() -> ledgerService.record(transaction, List.of(
                Posting.debit(source, new BigDecimal("100")),
                Posting.credit(target, new BigDecimal("99")))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not balanced");

        assertThat(source.getBalance()).isEqualByComparingTo("1000");
        verify(entries, never()).saveAll(anyList());
    }

    @Test
    void balancesEachCurrencySeparately() {
        Wallet usdSource = wallet(user("Alice Smith"), Currency.USD, "10");
        Wallet uzsTarget = wallet(user("Bob Brown"), Currency.UZS, "0");

        // 10 USD out and 120 000 UZS in only balance with the exchange wallets in between
        assertThatThrownBy(() -> ledgerService.record(transaction, List.of(
                Posting.debit(usdSource, new BigDecimal("10")),
                Posting.credit(uzsTarget, new BigDecimal("120000")))))
                .isInstanceOf(IllegalStateException.class);

        ledgerService.record(transaction, List.of(
                Posting.debit(usdSource, new BigDecimal("10")),
                Posting.credit(systemWallet(Currency.USD), new BigDecimal("10")),
                Posting.debit(systemWallet(Currency.UZS), new BigDecimal("120000")),
                Posting.credit(uzsTarget, new BigDecimal("120000"))));

        assertThat(usdSource.getBalance()).isEqualByComparingTo("0");
        assertThat(uzsTarget.getBalance()).isEqualByComparingTo("120000");
    }

    @Test
    void rejectsNonPositiveAmounts() {
        Wallet wallet = wallet(user("Alice Smith"), Currency.UZS, "0");

        assertThatThrownBy(() -> ledgerService.record(transaction, List.of(
                Posting.debit(wallet, BigDecimal.ZERO),
                Posting.credit(wallet, BigDecimal.ZERO))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Wallet systemWallet(Currency currency) {
        Wallet wallet = wallet(user("System"), currency, "0");
        ReflectionTestUtils.setField(wallet, "type", WalletType.FEE);
        ReflectionTestUtils.setField(wallet, "owner", null);
        ReflectionTestUtils.setField(wallet, "balance", null);
        return wallet;
    }
}
