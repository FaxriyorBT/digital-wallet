package uz.digitalwallet.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Sends saved outbox events to Kafka. Delivery is at-least-once: if the app stops after Kafka accepted
 * a message but before the row is marked as published, the message is sent again, so consumers must be idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository events;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties properties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = events.lockNextBatch(properties.batchSize());
        for (OutboxEvent event : batch) {
            try {
                // the aggregate id is the message key, so events of one transfer land in one partition in order
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
                event.markPublished(clock.instant());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | TimeoutException e) {
                event.markFailed(e.getMessage());
                log.warn("Could not publish outbox event {} to {}: {}", event.getId(), event.getTopic(), e.getMessage());
                // stop the batch: newer events must not overtake the one that failed
                return;
            }
        }
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "${app.time-zone}")
    @Transactional
    public void deletePublishedEvents() {
        int deleted = events.deletePublishedBefore(clock.instant().minus(properties.retention()));
        if (deleted > 0) {
            log.info("Deleted {} published outbox events", deleted);
        }
    }
}
