package uz.digitalwallet.exchange;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.exchange")
public record ExchangeProperties(String cbuUrl, Duration connectTimeout, Duration readTimeout) {
}
