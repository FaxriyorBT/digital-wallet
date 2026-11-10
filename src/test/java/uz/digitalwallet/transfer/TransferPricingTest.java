package uz.digitalwallet.transfer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.exchange.ExchangeRateService;
import uz.digitalwallet.user.User;
import uz.digitalwallet.wallet.Wallet;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uz.digitalwallet.support.TestData.user;
import static uz.digitalwallet.support.TestData.wallet;

@ExtendWith(MockitoExtension.class)
class TransferPricingTest {

    @Mock
    private ExchangeRateService exchangeRates;

    private TransferPricing pricing;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        var limits = new TransferProperties.Limits(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);
        var properties = new TransferProperties(new BigDecimal("0.5"), Map.of(Currency.UZS, limits));
        pricing = new TransferPricing(properties, exchangeRates);
        alice = user("Alice Smith");
        bob = user("Bob Brown");
    }

    @Test
    void chargesFeeForTransferToAnotherUser() {
        TransferQuote quote = pricing.quote(wallet(alice, Currency.UZS, "0"), wallet(bob, Currency.UZS, "0"),
                new BigDecimal("100000.00"));

        assertThat(quote.fee()).isEqualByComparingTo("500.00");
        assertThat(quote.totalDebit()).isEqualByComparingTo("100500.00");
        assertThat(quote.creditedAmount()).isEqualByComparingTo("100000.00");
        assertThat(quote.exchangeRate()).isNull();
    }

    @Test
    void transferBetweenOwnWalletsIsFree() {
        TransferQuote quote = pricing.quote(wallet(alice, Currency.UZS, "0"), wallet(alice, Currency.UZS, "0"),
                new BigDecimal("100000.00"));

        assertThat(quote.fee()).isEqualByComparingTo("0");
        assertThat(quote.totalDebit()).isEqualByComparingTo("100000.00");
    }

    @Test
    void feeIsRoundedWithBankersRounding() {
        // 0.5% of 1001.00 is 5.005, HALF_EVEN rounds to the even neighbour 5.00
        TransferQuote quote = pricing.quote(wallet(alice, Currency.UZS, "0"), wallet(bob, Currency.UZS, "0"),
                new BigDecimal("1001.00"));

        assertThat(quote.fee()).isEqualByComparingTo("5.00");
    }

    @Test
    void convertsUsdToUzsWithOfficialRate() {
        when(exchangeRates.rateInUzs(Currency.USD)).thenReturn(new BigDecimal("12000.00"));
        when(exchangeRates.rateInUzs(Currency.UZS)).thenReturn(BigDecimal.ONE);

        TransferQuote quote = pricing.quote(wallet(alice, Currency.USD, "0"), wallet(bob, Currency.UZS, "0"),
                new BigDecimal("10.00"));

        assertThat(quote.creditedAmount()).isEqualByComparingTo("120000.00");
        assertThat(quote.creditedCurrency()).isEqualTo(Currency.UZS);
        assertThat(quote.fee()).isEqualByComparingTo("0.05");
        assertThat(quote.exchangeRate()).isEqualByComparingTo("12000");
    }

    @Test
    void convertsUzsToUsdAndRoundsToCents() {
        when(exchangeRates.rateInUzs(Currency.UZS)).thenReturn(BigDecimal.ONE);
        when(exchangeRates.rateInUzs(Currency.USD)).thenReturn(new BigDecimal("12000.00"));

        TransferQuote quote = pricing.quote(wallet(alice, Currency.UZS, "0"), wallet(alice, Currency.USD, "0"),
                new BigDecimal("100000.00"));

        // 100 000 / 12 000 = 8.3333...
        assertThat(quote.creditedAmount()).isEqualByComparingTo("8.33");
    }

    @Test
    void rejectsAmountThatBecomesZeroAfterConversion() {
        when(exchangeRates.rateInUzs(Currency.UZS)).thenReturn(BigDecimal.ONE);
        when(exchangeRates.rateInUzs(Currency.USD)).thenReturn(new BigDecimal("12000.00"));

        Wallet source = wallet(alice, Currency.UZS, "0");
        Wallet target = wallet(alice, Currency.USD, "0");

        assertThatThrownBy(() -> pricing.quote(source, target, new BigDecimal("10.00")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AMOUNT_OUT_OF_RANGE);
    }
}
