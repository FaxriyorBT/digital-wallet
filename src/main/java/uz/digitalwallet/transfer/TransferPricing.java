package uz.digitalwallet.transfer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.money.Money;
import uz.digitalwallet.exchange.ExchangeRateService;
import uz.digitalwallet.wallet.Wallet;

import java.math.BigDecimal;

/**
 * Single place where fee and conversion are calculated, used both for the quote and for the real transfer,
 * so the user is charged exactly what the quote showed.
 */
@Component
@RequiredArgsConstructor
public class TransferPricing {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int RATE_SCALE = 10;

    private final TransferProperties properties;
    private final ExchangeRateService exchangeRates;

    public TransferQuote quote(Wallet source, Wallet target, BigDecimal amount) {
        BigDecimal fee = calculateFee(source, target, amount);
        if (source.getCurrency() == target.getCurrency()) {
            return TransferQuote.of(amount, source.getCurrency(), fee, amount, target.getCurrency(), null);
        }

        BigDecimal sourceRate = exchangeRates.rateInUzs(source.getCurrency());
        BigDecimal targetRate = exchangeRates.rateInUzs(target.getCurrency());
        // both rates are "UZS per unit", so the conversion goes through UZS: amount * sourceRate / targetRate
        BigDecimal credited = amount.multiply(sourceRate).divide(targetRate, Money.SCALE, Money.ROUNDING);
        if (credited.signum() <= 0) {
            throw new BusinessException(ErrorCode.AMOUNT_OUT_OF_RANGE, "Amount is too small to be converted");
        }
        BigDecimal rate = sourceRate.divide(targetRate, RATE_SCALE, Money.ROUNDING);
        return TransferQuote.of(amount, source.getCurrency(), fee, credited, target.getCurrency(), rate);
    }

    private BigDecimal calculateFee(Wallet source, Wallet target, BigDecimal amount) {
        boolean ownWallets = source.getOwner().getId().equals(target.getOwner().getId());
        if (ownWallets) {
            return Money.zero();
        }
        return Money.round(amount.multiply(properties.feePercent()).divide(ONE_HUNDRED));
    }
}
