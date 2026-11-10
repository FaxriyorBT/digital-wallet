package uz.digitalwallet.exchange;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.exchange.dto.ExchangeRateResponse;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    static final String CACHE_NAME = "exchange-rates";

    private final ExchangeRateRepository rates;
    private final Clock clock;

    /**
     * Read on every cross-currency transfer but changes once a day, a good fit for a cache.
     * Callers must use this through the Spring proxy (another bean), otherwise @Cacheable is skipped.
     */
    @Cacheable(cacheNames = CACHE_NAME, key = "#currency")
    @Transactional(readOnly = true)
    public BigDecimal rateInUzs(Currency currency) {
        if (currency == Currency.UZS) {
            return BigDecimal.ONE;
        }
        return rates.findFirstByCurrencyOrderByRateDateDesc(currency)
                .map(ExchangeRate::getRate)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCHANGE_RATE_UNAVAILABLE));
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateResponse> latestRates() {
        return Arrays.stream(Currency.values())
                .filter(currency -> currency != Currency.UZS)
                .map(rates::findFirstByCurrencyOrderByRateDateDesc)
                .flatMap(Optional::stream)
                .map(rate -> new ExchangeRateResponse(rate.getCurrency(), rate.getRate(), rate.getRateDate()))
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
    public int saveRates(List<CbuRate> cbuRates) {
        Instant now = clock.instant();
        int saved = 0;
        for (CbuRate cbuRate : cbuRates) {
            Optional<Currency> currency = Currency.fromCode(cbuRate.currencyCode())
                    .filter(code -> code != Currency.UZS);
            if (currency.isEmpty() || rates.existsByCurrencyAndRateDate(currency.get(), cbuRate.rateDate())) {
                continue;
            }
            rates.save(new ExchangeRate(currency.get(), cbuRate.ratePerUnit(), cbuRate.rateDate(), now));
            saved++;
        }
        return saved;
    }
}
