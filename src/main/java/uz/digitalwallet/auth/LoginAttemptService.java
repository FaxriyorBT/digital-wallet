package uz.digitalwallet.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;

/**
 * Counts failed logins per email in Redis. After too many failures the email is locked until the key expires,
 * which slows down password guessing. Redis is used instead of the database because the counter is shared
 * by all application instances and expires on its own.
 */
@Service
@RequiredArgsConstructor
class LoginAttemptService {

    private static final String KEY_PREFIX = "login-failures:";

    private final StringRedisTemplate redis;
    private final LoginProperties properties;

    void checkNotLocked(String email) {
        String failures = redis.opsForValue().get(key(email));
        if (failures != null && Integer.parseInt(failures) >= properties.maxAttempts()) {
            throw new BusinessException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        }
    }

    void registerFailure(String email) {
        String key = key(email);
        redis.opsForValue().increment(key);
        // every failure moves the expiry forward, so the lock lasts lockDuration after the last attempt
        redis.expire(key, properties.lockDuration());
    }

    void reset(String email) {
        redis.delete(key(email));
    }

    private static String key(String email) {
        return KEY_PREFIX + email;
    }
}
