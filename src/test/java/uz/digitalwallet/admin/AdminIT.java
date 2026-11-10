package uz.digitalwallet.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import uz.digitalwallet.support.IntegrationTest;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uz.digitalwallet.common.money.Currency.UZS;

class AdminIT extends IntegrationTest {

    private String adminBearer;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        adminBearer = "Bearer " + login("admin@wallet.local", "Admin12345").get("accessToken").asString();
    }

    @Test
    void regularUserCannotUseAdminApi() throws Exception {
        TestUser user = registerUser("Alice Karimova");

        mvc.perform(get("/api/v1/admin/users").header("Authorization", user.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void blockedUserCannotLoginRefreshOrReceiveMoney() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode bobWallet = openWallet(bob, UZS);
        topUp(alice, aliceWallet, "100000");

        perform(patch("/api/v1/admin/users/{id}/status", bob.id()).header("Authorization", adminBearer),
                Map.of("status", "BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        perform(post("/api/v1/auth/login"), Map.of("email", bob.email(), "password", "Secret123"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_BLOCKED"));
        perform(post("/api/v1/auth/refresh"), Map.of("refreshToken", bob.refreshToken()))
                .andExpect(status().isUnauthorized());
        transfer(alice, UUID.randomUUID().toString(), aliceWallet, bobWallet, "10000")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("RECIPIENT_UNAVAILABLE"));
    }

    @Test
    void frozenWalletCannotSendMoney() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode bobWallet = openWallet(bob, UZS);
        topUp(alice, aliceWallet, "100000");

        perform(patch("/api/v1/admin/wallets/{id}/status", aliceWallet.get("id").asString())
                        .header("Authorization", adminBearer),
                Map.of("status", "FROZEN"))
                .andExpect(status().isOk());

        transfer(alice, UUID.randomUUID().toString(), aliceWallet, bobWallet, "10000")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("WALLET_NOT_ACTIVE"));
    }

    @Test
    void administratorsCannotBeBlocked() throws Exception {
        JsonNode admins = body(mvc.perform(get("/api/v1/admin/users").param("query", "admin@wallet.local")
                .header("Authorization", adminBearer)).andExpect(status().isOk()));
        String adminId = admins.get("content").get(0).get("id").asString();

        perform(patch("/api/v1/admin/users/{id}/status", adminId).header("Authorization", adminBearer),
                Map.of("status", "BLOCKED"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("STATUS_CHANGE_NOT_ALLOWED"));
    }

    @Test
    void dailyStatsIncludeTodaysTransfers() throws Exception {
        TestUser alice = registerUser("Alice Karimova");
        TestUser bob = registerUser("Bob Toshmatov");
        JsonNode aliceWallet = openWallet(alice, UZS);
        JsonNode bobWallet = openWallet(bob, UZS);
        topUp(alice, aliceWallet, "100000");
        transfer(alice, UUID.randomUUID().toString(), aliceWallet, bobWallet, "10000").andExpect(status().isCreated());

        mvc.perform(get("/api/v1/admin/stats/daily").header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnover[?(@.type == 'TRANSFER' && @.currency == 'UZS')].count").exists());
    }
}
