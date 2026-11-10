package uz.digitalwallet.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.security.login")
public record LoginProperties(int maxAttempts, Duration lockDuration) {
}
