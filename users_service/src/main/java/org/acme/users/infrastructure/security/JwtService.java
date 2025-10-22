package org.acme.users.infrastructure.security;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.users.domain.model.User;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@ApplicationScoped
public class JwtService {

    private final String issuer;
    private final Duration tokenDuration;
    private final String secret;

    public JwtService(
            @org.eclipse.microprofile.config.inject.ConfigProperty(name = "jwt.issuer", defaultValue = "users-service") String issuer,
            @org.eclipse.microprofile.config.inject.ConfigProperty(name = "jwt.expiration.minutes", defaultValue = "15") long expirationMinutes,
            @org.eclipse.microprofile.config.inject.ConfigProperty(name = "jwt.secret") String secret
    ) {
        this.issuer = issuer;
        this.tokenDuration = Duration.ofMinutes(expirationMinutes);
        this.secret = secret;
    }

    public TokenWithExpiry issueToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(tokenDuration);

        String token = Jwt.issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(expiry)
                .upn(user.getUsername())
                .groups(Set.of(user.getRole().name()))
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("userId", user.getId())
                .signWithSecret(secret);

        return new TokenWithExpiry(token, tokenDuration);
    }

    public record TokenWithExpiry(String token, Duration duration) {
    }
}
