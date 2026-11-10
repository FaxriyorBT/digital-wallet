package uz.digitalwallet.exchange;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Client for the public rates feed of the Central Bank of Uzbekistan.
 * Retry handles short network glitches; if CBU keeps failing, the circuit breaker opens
 * and calls fail fast for a while instead of waiting for timeouts again and again.
 */
@Component
@RequiredArgsConstructor
public class CbuClient {

    private static final String RATES_PATH = "/uz/arkhiv-kursov-valyut/json/";

    private final RestClient cbuRestClient;

    @Retry(name = "cbu")
    @CircuitBreaker(name = "cbu")
    public List<CbuRate> fetchLatestRates() {
        List<CbuRate> rates = cbuRestClient.get()
                .uri(RATES_PATH)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return rates != null ? rates : List.of();
    }
}
