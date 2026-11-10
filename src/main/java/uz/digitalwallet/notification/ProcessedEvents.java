package uz.digitalwallet.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

/**
 * Makes the consumer idempotent. Kafka delivers at least once, so the same event can arrive twice
 * (after a rebalance, or when the outbox resends it).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ProcessedEvents {

    private final JdbcClient jdbc;
    private final Clock clock;

    /**
     * The "already processed" mark and the action share one transaction: if the action throws,
     * the mark is rolled back and the redelivered message is handled again.
     */
    @Transactional
    public void runOnce(UUID eventId, Runnable action) {
        int inserted = jdbc.sql("""
                        INSERT INTO processed_events (event_id, processed_at)
                        VALUES (:eventId, :processedAt)
                        ON CONFLICT (event_id) DO NOTHING
                        """)
                .param("eventId", eventId)
                .param("processedAt", Timestamp.from(clock.instant()))
                .update();
        if (inserted == 0) {
            log.info("Event {} was already processed, skipping it", eventId);
            return;
        }
        action.run();
    }
}
