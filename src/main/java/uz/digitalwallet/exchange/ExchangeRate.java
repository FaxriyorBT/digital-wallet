package uz.digitalwallet.exchange;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import uz.digitalwallet.common.money.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Official rate for one day: how many UZS one unit of the currency costs.
 */
@Entity
@Table(name = "exchange_rates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRate {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    private BigDecimal rate;

    private LocalDate rateDate;

    private Instant fetchedAt;

    public ExchangeRate(Currency currency, BigDecimal rate, LocalDate rateDate, Instant fetchedAt) {
        this.currency = currency;
        this.rate = rate;
        this.rateDate = rateDate;
        this.fetchedAt = fetchedAt;
    }
}
