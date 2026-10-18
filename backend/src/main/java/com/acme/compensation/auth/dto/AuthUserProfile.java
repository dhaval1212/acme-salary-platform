package com.acme.compensation.auth.dto;

import java.util.UUID;

/**
 * Safe user profile — never includes password hash, signing secrets,
 * or any other sensitive internal data.
 */
public record AuthUserProfile(
        UUID id,
        String username,
        String fullName,
        String role
) {
}
