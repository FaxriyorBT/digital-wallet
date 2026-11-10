package uz.digitalwallet.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import uz.digitalwallet.auth.dto.TokenResponse;
import uz.digitalwallet.common.security.JwtProperties;
import uz.digitalwallet.user.Role;
import uz.digitalwallet.user.User;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final RefreshTokenService refreshTokens;
    private final Clock clock;

    public TokenResponse issueTokens(UUID userId, Role role) {
        return TokenResponse.bearer(createAccessToken(userId, role), refreshTokens.issue(userId),
                properties.accessTokenTtl());
    }

    public TokenResponse refresh(String refreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(refreshToken);
        User user = rotation.user();
        return TokenResponse.bearer(createAccessToken(user.getId(), user.getRole()), rotation.refreshToken(),
                properties.accessTokenTtl());
    }

    private String createAccessToken(UUID userId, Role role) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .claim("roles", List.of(role.name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
