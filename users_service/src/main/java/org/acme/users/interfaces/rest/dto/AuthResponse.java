package org.acme.users.interfaces.rest.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn
) {
    public static AuthResponse bearer(String token, long expiresInSeconds) {
        return new AuthResponse(token, "Bearer", expiresInSeconds);
    }
}
