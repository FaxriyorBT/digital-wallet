package uz.digitalwallet.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.security.JwtProperties;
import uz.digitalwallet.user.User;
import uz.digitalwallet.user.UserAccessRevokedEvent;
import uz.digitalwallet.user.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final Duration STALE_TOKEN_RETENTION = Duration.ofDays(7);

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public String issue(UUID userId) {
        String rawToken = generateToken();
        Instant expiresAt = clock.instant().plus(jwtProperties.refreshTokenTtl());
        tokens.save(RefreshToken.issue(users.getReferenceById(userId), hash(rawToken), expiresAt));
        return rawToken;
    }

    /**
     * Exchanges a refresh token for a new one. Each token works only once: if an already used token
     * comes back, it was most likely stolen, so every session of that user is closed.
     * The revocation must be committed even though we answer with an error, hence noRollbackFor.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public Rotation rotate(String rawToken) {
        RefreshToken token = tokens.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        Instant now = clock.instant();
        User user = token.getUser();

        if (token.isRevoked()) {
            log.warn("Reuse of a revoked refresh token detected for user {}, closing all sessions", user.getId());
            tokens.revokeAllActive(user.getId(), now);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (token.isExpired(now)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        token.revoke(now);
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_BLOCKED);
        }
        return new Rotation(user, issue(user.getId()));
    }

    @Transactional
    public void revoke(String rawToken) {
        tokens.findByTokenHash(hash(rawToken)).ifPresent(token -> token.revoke(clock.instant()));
    }

    @EventListener
    @Transactional
    public void onAccessRevoked(UserAccessRevokedEvent event) {
        int revoked = tokens.revokeAllActive(event.userId(), clock.instant());
        log.info("Revoked {} refresh token(s) of user {}", revoked, event.userId());
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "${app.time-zone}")
    @Transactional
    public void deleteStaleTokens() {
        int deleted = tokens.deleteStale(clock.instant().minus(STALE_TOKEN_RETENTION));
        if (deleted > 0) {
            log.info("Deleted {} expired or revoked refresh tokens", deleted);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // a fast hash is fine here: the token is 256 random bits, unlike a password it cannot be guessed
    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public record Rotation(User user, String refreshToken) {
    }
}
