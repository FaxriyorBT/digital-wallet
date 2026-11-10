package uz.digitalwallet.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Creates the first administrator from configuration. The password comes from an environment variable,
 * so no credentials end up in migrations or in the repository.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class AdminAccountInitializer implements ApplicationRunner {

    private final AdminProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(properties.password())) {
            log.warn("ADMIN_PASSWORD is not set, administrator account was not created");
            return;
        }
        String email = User.normalizeEmail(properties.email());
        if (users.existsByEmail(email)) {
            return;
        }
        String passwordHash = passwordEncoder.encode(properties.password());
        users.save(User.create(email, properties.phone(), passwordHash, properties.fullName(), Role.ADMIN));
        log.info("Administrator account {} created", email);
    }
}
