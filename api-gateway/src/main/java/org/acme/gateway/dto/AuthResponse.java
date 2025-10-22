package org.acme.gateway.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(
        @JsonProperty("access_token")
        @JsonAlias("token")
        String accessToken,

        @JsonProperty("token_type")
        @JsonAlias("tokenType")
        String tokenType,

        @JsonProperty("expires_in")
        @JsonAlias("expiresIn")
        Long expiresIn
) {
    public static AuthResponse bearer(String token, Long expiresIn) {
        return new AuthResponse(token, "Bearer", expiresIn);
    }
}
