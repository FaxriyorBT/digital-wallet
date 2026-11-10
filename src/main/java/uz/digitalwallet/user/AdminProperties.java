package uz.digitalwallet.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.admin")
public record AdminProperties(String email, String password, String fullName, String phone) {
}
