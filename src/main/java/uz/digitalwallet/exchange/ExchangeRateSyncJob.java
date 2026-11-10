package uz.digitalwallet.exchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.exchange.sync-enabled", havingValue = "true", matchIfMissing = true)
public class ExchangeRateSyncJob {

    private final CbuClient cbuClient;
    private final ExchangeRateService exchangeRates;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        sync();
    }

    @Scheduled(cron = "${app.exchange.sync-cron}", zone = "${app.time-zone}")
    public void sync() {
        try {
            int saved = exchangeRates.saveRates(cbuClient.fetchLatestRates());
            log.info("Exchange rates synchronized with CBU, {} new rate(s) saved", saved);
        } catch (Exception e) {
            // the last known rates stay in use, the next scheduled run tries again
            log.warn("Could not synchronize exchange rates with CBU: {}", e.toString());
        }
    }
}
