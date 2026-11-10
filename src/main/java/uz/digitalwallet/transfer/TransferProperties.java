package uz.digitalwallet.transfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import uz.digitalwallet.common.money.Currency;

import java.math.BigDecimal;
import java.util.Map;

@Validated
@ConfigurationProperties("app.transfer")
public record TransferProperties(
        @NotNull @DecimalMin("0") BigDecimal feePercent,
        @NotEmpty Map<Currency, Limits> limits) {

    public Limits limitsFor(Currency currency) {
        Limits currencyLimits = limits.get(currency);
        if (currencyLimits == null) {
            throw new IllegalStateException("No limits configured for " + currency);
        }
        return currencyLimits;
    }

    public record Limits(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal dailyAmount, BigDecimal maxTopUp) {
    }
}
