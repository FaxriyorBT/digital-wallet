package uz.digitalwallet.exchange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Checks the HTTP call and the parsing of the CBU format. Retry and circuit breaker are applied
 * by Spring proxies and are not active when the client is created with "new".
 */
class CbuClientTest {

    private static final String RATES_URL = "https://cbu.uz/uz/arkhiv-kursov-valyut/json/";

    private MockRestServiceServer server;
    private CbuClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://cbu.uz");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CbuClient(builder.build());
    }

    @Test
    void parsesRatesAndNominal() {
        server.expect(requestTo(RATES_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {"id":1,"Code":"840","Ccy":"USD","CcyNm_EN":"US Dollar","Nominal":"1","Rate":"11987.16","Diff":"13.68","Date":"07.11.2026"},
                          {"id":2,"Code":"392","Ccy":"JPY","CcyNm_EN":"Japanese Yen","Nominal":"10","Rate":"785.30","Diff":"1.10","Date":"07.11.2026"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<CbuRate> rates = client.fetchLatestRates();

        assertThat(rates).hasSize(2);
        assertThat(rates.getFirst().currencyCode()).isEqualTo("USD");
        assertThat(rates.getFirst().ratePerUnit()).isEqualByComparingTo(new BigDecimal("11987.16"));
        assertThat(rates.getFirst().rateDate()).isEqualTo(LocalDate.of(2026, 11, 7));
        assertThat(rates.get(1).ratePerUnit()).isEqualByComparingTo(new BigDecimal("78.53"));
        server.verify();
    }

    @Test
    void propagatesServerErrorsSoRetryCanHandleThem() {
        server.expect(requestTo(RATES_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchLatestRates()).isInstanceOf(HttpServerErrorException.class);
    }
}
