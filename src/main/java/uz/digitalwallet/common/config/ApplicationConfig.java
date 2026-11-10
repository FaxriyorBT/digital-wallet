package uz.digitalwallet.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ApplicationConfig {

    /**
     * Instants are stored in UTC, but "today" (daily limits, statements, reports) is the business day in Tashkent.
     * Injecting the clock instead of calling Instant.now() also makes time-dependent code testable.
     */
    @Bean
    public Clock clock(@Value("${app.time-zone}") ZoneId zone) {
        return Clock.system(zone);
    }
}
