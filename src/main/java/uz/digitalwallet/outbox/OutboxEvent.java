package uz.digitalwallet.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private String aggregateType;

    private String aggregateId;

    private String eventType;

    private String topic;

    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String payload;

    private Instant createdAt;

    private Instant publishedAt;

    private int attempts;

    private String lastError;

    static OutboxEvent create(String topic, String aggregateType, String aggregateId, String eventType,
                              String payload, Instant createdAt) {
        OutboxEvent event = new OutboxEvent();
        event.topic = topic;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.payload = payload;
        event.createdAt = createdAt;
        return event;
    }

    void markPublished(Instant now) {
        publishedAt = now;
        attempts++;
        lastError = null;
    }

    void markFailed(String error) {
        attempts++;
        lastError = error == null || error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }
}
