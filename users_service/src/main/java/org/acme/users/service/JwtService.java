package org.acme.users.service;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.users.domain.UserEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@ApplicationScoped
public class JwtService {

    private final String issuer;
    private final Duration tokenDuration;

    public JwtService(
            @org.eclipse.microprofile.config.inject.ConfigProperty(name = "jwt.issuer", defaultValue = "users-service") String issuer,
            @org.eclipse.microprofile.config.inject.ConfigProperty(name = "jwt.expiration.minutes", defaultValue = "15") long expirationMinutes
    ) {
        this.issuer = issuer;
        this.tokenDuration = Duration.ofMinutes(expirationMinutes);
    }

    public TokenWithExpiry issueToken(UserEntity user) {
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
                .sign();

        return new TokenWithExpiry(token, tokenDuration);
    }

    public record TokenWithExpiry(String token, Duration duration) {
    }
}
