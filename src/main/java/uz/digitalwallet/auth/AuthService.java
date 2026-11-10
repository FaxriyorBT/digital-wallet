package uz.digitalwallet.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.digitalwallet.auth.dto.LoginRequest;
import uz.digitalwallet.auth.dto.RegisterRequest;
import uz.digitalwallet.auth.dto.TokenResponse;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.event.Topics;
import uz.digitalwallet.event.UserRegisteredEvent;
import uz.digitalwallet.outbox.OutboxService;
import uz.digitalwallet.user.Role;
import uz.digitalwallet.user.User;
import uz.digitalwallet.user.UserMapper;
import uz.digitalwallet.user.UserRepository;
import uz.digitalwallet.user.dto.UserResponse;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttempts;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokens;
    private final OutboxService outbox;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        if (users.existsByPhone(request.phone())) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }
        String passwordHash = passwordEncoder.encode(request.password());
        User user = users.save(User.create(email, request.phone(), passwordHash, request.fullName().trim(), Role.USER));

        var event = new UserRegisteredEvent(UUID.randomUUID(), user.getId(), user.getEmail(), user.getFullName(),
                user.getCreatedAt());
        outbox.enqueue(Topics.USER_REGISTERED, "User", user.getId().toString(), event);
        log.info("User {} registered", user.getId());
        return userMapper.toResponse(user);
    }

    public TokenResponse login(LoginRequest request) {
        String email = User.normalizeEmail(request.email());
        loginAttempts.checkNotLocked(email);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (BadCredentialsException e) {
            loginAttempts.registerFailure(email);
            throw e;
        }
        loginAttempts.reset(email);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return tokenService.issueTokens(principal.getId(), principal.getRole());
    }

    public TokenResponse refresh(String refreshToken) {
        return tokenService.refresh(refreshToken);
    }

    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }
}
