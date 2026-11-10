package uz.digitalwallet.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.money.Currency;
import uz.digitalwallet.common.security.JwtProperties;
import uz.digitalwallet.common.security.SecurityConfig;
import uz.digitalwallet.common.security.SecurityExceptionDelegate;
import uz.digitalwallet.wallet.dto.WalletResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer only: real controller, security rules, validation and error handling; the service is a mock.
 */
@WebMvcTest(WalletController.class)
@Import({SecurityConfig.class, SecurityExceptionDelegate.class, WalletControllerTest.JwtConfig.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-only-secret-key-that-is-long-enough-for-hs256",
        "app.security.jwt.issuer=digital-wallet",
        "app.security.jwt.access-token-ttl=15m",
        "app.security.jwt.refresh-token-ttl=30d"
})
class WalletControllerTest {

    private final UUID userId = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mvc.perform(get("/api/v1/wallets"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void opensWalletAndReturnsItsLocation() throws Exception {
        UUID walletId = UUID.randomUUID();
        when(walletService.open(userId, Currency.USD)).thenReturn(new WalletResponse(walletId, "4000000000000002",
                Currency.USD, new BigDecimal("0.00"), WalletStatus.ACTIVE, Instant.now()));

        mvc.perform(post("/api/v1/wallets").with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"USD\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/wallets/" + walletId))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(walletService).open(userId, Currency.USD);
    }

    @Test
    void unknownCurrencyIsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/wallets").with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"GBP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void walletNumberWithWrongCheckDigitIsRejected() throws Exception {
        mvc.perform(get("/api/v1/wallets/lookup").param("number", "4000000000000003").with(authenticatedUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.number").exists());
    }

    @Test
    void businessErrorBecomesProblemDetail() throws Exception {
        when(walletService.get(any(), any())).thenThrow(new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        mvc.perform(get("/api/v1/wallets/{id}", UUID.randomUUID()).with(authenticatedUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WALLET_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Wallet not found"))
                .andExpect(header().exists("X-Request-Id"));
    }

    private RequestPostProcessor authenticatedUser() {
        return jwt().jwt(token -> token.subject(userId.toString()));
    }

    @TestConfiguration
    @EnableConfigurationProperties(JwtProperties.class)
    static class JwtConfig {
    }
}
