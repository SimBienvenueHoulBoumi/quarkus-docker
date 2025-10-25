package org.acme.gateway.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        @JsonProperty("created_at")
        Instant createdAt
) {
}
