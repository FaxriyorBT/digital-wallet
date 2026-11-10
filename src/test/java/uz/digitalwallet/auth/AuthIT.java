package uz.digitalwallet.auth;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import uz.digitalwallet.support.IntegrationTest;
import uz.digitalwallet.support.TestData;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends IntegrationTest {

    @Test
    void registeredUserCanReadOwnProfile() throws Exception {
        TestUser user = registerUser("Aziza Rahimova");

        mvc.perform(get("/api/v1/users/me").header("Authorization", user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.email()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void emailIsUniqueIgnoringCase() throws Exception {
        TestUser user = registerUser("Aziza Rahimova");

        perform(post("/api/v1/auth/register"), Map.of(
                "email", user.email().toUpperCase(),
                "phone", TestData.randomPhone(),
                "password", "Secret123",
                "fullName", "Someone Else"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        perform(post("/api/v1/auth/register"), Map.of(
                "email", "not-an-email", "phone", "901234567", "password", "password", "fullName", "A"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    void refreshTokenWorksOnlyOnce() throws Exception {
        TestUser user = registerUser("Aziza Rahimova");

        JsonNode rotated = body(perform(post("/api/v1/auth/refresh"), Map.of("refreshToken", user.refreshToken()))
                .andExpect(status().isOk()));

        // the used token shows up again: it is treated as stolen and the whole session family is revoked
        perform(post("/api/v1/auth/refresh"), Map.of("refreshToken", user.refreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        perform(post("/api/v1/auth/refresh"), Map.of("refreshToken", rotated.get("refreshToken").asString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        TestUser user = registerUser("Aziza Rahimova");

        perform(post("/api/v1/auth/logout"), Map.of("refreshToken", user.refreshToken()))
                .andExpect(status().isNoContent());
        perform(post("/api/v1/auth/refresh"), Map.of("refreshToken", user.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginIsLockedAfterTooManyFailures() throws Exception {
        TestUser user = registerUser("Aziza Rahimova");

        for (int attempt = 0; attempt < 5; attempt++) {
            perform(post("/api/v1/auth/login"), Map.of("email", user.email(), "password", "Wrong1234"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        // even the correct password is refused while the lock is active
        perform(post("/api/v1/auth/login"), Map.of("email", user.email(), "password", "Secret123"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_LOGIN_ATTEMPTS"));
    }

    @Test
    void changingPasswordEndsExistingSessions() throws Exception {
        TestUser user = registerUser("Aziza Rahimova");

        perform(put("/api/v1/users/me/password").header("Authorization", user.bearer()),
                Map.of("currentPassword", "Secret123", "newPassword", "NewSecret456"))
                .andExpect(status().isNoContent());

        perform(post("/api/v1/auth/refresh"), Map.of("refreshToken", user.refreshToken()))
                .andExpect(status().isUnauthorized());
        login(user.email(), "NewSecret456");
    }

    @Test
    void wrongCurrentPasswordIsRejected() throws Exception {
        TestUser user = registerUser("Aziza Rahimova");

        perform(put("/api/v1/users/me/password").header("Authorization", user.bearer()),
                Map.of("currentPassword", "Wrong1234", "newPassword", "NewSecret456"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("WRONG_CURRENT_PASSWORD"));
    }
}
