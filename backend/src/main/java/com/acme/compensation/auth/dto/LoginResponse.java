package com.acme.compensation.auth.dto;

import java.time.Instant;

/**
 * Response body for a successful login.
 * Never includes the password hash, signing secret, or raw token claims.
 */
public record LoginResponse(
        String token,
        Instant expiresAt,
        AuthUserProfile user
) {
}
