package org.acme.orders.infrastructure.security;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.crypto.spec.SecretKeySpec;

@ApplicationScoped
@Alternative
@Priority(1)
public class JwtAuthContextProducer {

    @ConfigProperty(name = "jwt.secret")
    String secret;

    @ConfigProperty(name = "jwt.issuer")
    String issuer;

    @ConfigProperty(name = "jwt.algorithm", defaultValue = "HS256")
    String algorithm;

    @Produces
    @Singleton
    public JWTAuthContextInfo jwtAuthContextInfo() {
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), toJcaAlgorithm(algorithm));
        JWTAuthContextInfo info = new JWTAuthContextInfo(secretKey, issuer);
        info.setSignatureAlgorithm(Collections.singleton(SignatureAlgorithm.valueOf(algorithm)));
        return info;
    }

    private static String toJcaAlgorithm(String alg) {
        return switch (alg) {
            case "HS256" -> "HmacSHA256";
            case "HS384" -> "HmacSHA384";
            case "HS512" -> "HmacSHA512";
            default -> throw new IllegalArgumentException("Unsupported HMAC algorithm: " + alg);
        };
    }
}
