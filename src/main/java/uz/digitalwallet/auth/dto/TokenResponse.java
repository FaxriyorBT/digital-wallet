package uz.digitalwallet.auth.dto;

import java.time.Duration;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn) {

    public static TokenResponse bearer(String accessToken, String refreshToken, Duration accessTokenTtl) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", accessTokenTtl.toSeconds());
    }
}
