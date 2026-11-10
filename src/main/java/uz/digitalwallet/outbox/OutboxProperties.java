package uz.digitalwallet.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.outbox")
public record OutboxProperties(int batchSize, Duration sendTimeout, Duration retention) {
}
