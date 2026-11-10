package uz.digitalwallet.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.notification")
public record NotificationProperties(String from) {
}
