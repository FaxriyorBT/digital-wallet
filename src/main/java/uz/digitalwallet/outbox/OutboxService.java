package uz.digitalwallet.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

/**
 * Transactional outbox: instead of sending to Kafka directly (which could succeed while the database
 * transaction rolls back, or the other way round), the event is saved in the same transaction as the
 * business change. {@link OutboxPublisher} delivers it afterwards.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository events;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    // MANDATORY: an event saved outside a business transaction would defeat the whole point of the outbox
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String topic, String aggregateType, String aggregateId, Object event) {
        String payload = jsonMapper.writeValueAsString(event);
        String eventType = event.getClass().getSimpleName();
        events.save(OutboxEvent.create(topic, aggregateType, aggregateId, eventType, payload, clock.instant()));
    }
}
