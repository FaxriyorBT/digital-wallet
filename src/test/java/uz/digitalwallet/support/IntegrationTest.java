package uz.digitalwallet.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.exchange.ExchangeRate;
import uz.digitalwallet.exchange.ExchangeRateRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for tests that start the whole application. All subclasses share one Spring context
 * (and therefore one set of containers), so every test creates its own users instead of cleaning the database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class IntegrationTest {

    protected static final BigDecimal USD_RATE = new BigDecimal("12000.0000");
    protected static final BigDecimal EUR_RATE = new BigDecimal("13000.0000");

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JsonMapper jsonMapper;

    @Autowired
    private ExchangeRateRepository exchangeRates;

    @MockitoBean
    protected JavaMailSender mailSender;

    @BeforeEach
    void ensureExchangeRates() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        if (!exchangeRates.existsByCurrencyAndRateDate(Currency.USD, date)) {
            exchangeRates.save(new ExchangeRate(Currency.USD, USD_RATE, date, Instant.now()));
            exchangeRates.save(new ExchangeRate(Currency.EUR, EUR_RATE, date, Instant.now()));
        }
    }

    protected TestUser registerUser(String fullName) throws Exception {
        String email = UUID.randomUUID().toString().substring(0, 12) + "@test.uz";
        String phone = TestData.randomPhone();
        JsonNode user = body(perform(post("/api/v1/auth/register"), Map.of(
                "email", email, "phone", phone, "password", TestUser.PASSWORD, "fullName", fullName))
                .andExpect(status().isCreated()));
        JsonNode tokens = login(email, TestUser.PASSWORD);
        return new TestUser(UUID.fromString(user.get("id").asString()), email, tokens.get("accessToken").asString(),
                tokens.get("refreshToken").asString());
    }

    protected JsonNode login(String email, String password) throws Exception {
        return body(perform(post("/api/v1/auth/login"), Map.of("email", email, "password", password))
                .andExpect(status().isOk()));
    }

    protected JsonNode openWallet(TestUser user, Currency currency) throws Exception {
        return body(perform(post("/api/v1/wallets").header("Authorization", user.bearer()),
                Map.of("currency", currency.name()))
                .andExpect(status().isCreated()));
    }

    protected JsonNode topUp(TestUser user, JsonNode wallet, String amount) throws Exception {
        return body(perform(post("/api/v1/wallets/{id}/top-ups", wallet.get("id").asString())
                        .header("Authorization", user.bearer())
                        .header("Idempotency-Key", UUID.randomUUID().toString()),
                Map.of("amount", new BigDecimal(amount)))
                .andExpect(status().isCreated()));
    }

    protected ResultActions transfer(TestUser user, String idempotencyKey, JsonNode source, JsonNode target,
                                     String amount) throws Exception {
        return perform(post("/api/v1/transfers")
                        .header("Authorization", user.bearer())
                        .header("Idempotency-Key", idempotencyKey),
                Map.of("sourceWalletId", source.get("id").asString(),
                        "targetWalletNumber", target.get("number").asString(),
                        "amount", new BigDecimal(amount)));
    }

    protected BigDecimal balanceOf(TestUser user, JsonNode wallet) throws Exception {
        JsonNode current = body(mvc.perform(get("/api/v1/wallets/{id}", wallet.get("id").asString())
                .header("Authorization", user.bearer())).andExpect(status().isOk()));
        return current.get("balance").decimalValue();
    }

    protected ResultActions perform(MockHttpServletRequestBuilder request, Object content) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(content)));
    }

    protected JsonNode body(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    public record TestUser(UUID id, String email, String accessToken, String refreshToken) {

        static final String PASSWORD = "Secret123";

        public String bearer() {
            return "Bearer " + accessToken;
        }
    }
}
