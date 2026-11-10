package uz.digitalwallet.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.digitalwallet.admin.dto.DailyStatsResponse;
import uz.digitalwallet.transaction.TransactionRepository;
import uz.digitalwallet.transaction.TransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final TransactionRepository transactions;
    private final Clock clock;

    public DailyStatsResponse daily(LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now(clock);
        ZoneId zone = clock.getZone();
        Instant from = day.atStartOfDay(zone).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();
        return new DailyStatsResponse(day, transactions.turnover(TransactionStatus.COMPLETED, from, to));
    }
}
