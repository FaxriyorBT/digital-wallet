package uz.digitalwallet.exchange;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.digitalwallet.common.money.Currency;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    Optional<ExchangeRate> findFirstByCurrencyOrderByRateDateDesc(Currency currency);

    boolean existsByCurrencyAndRateDate(Currency currency, LocalDate rateDate);
}
