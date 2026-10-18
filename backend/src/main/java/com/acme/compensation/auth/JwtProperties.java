package com.acme.compensation.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT configuration bound from application properties.
 *
 * Required properties (via environment variables in production):
 *   jwt.secret       — HMAC-SHA256 signing key, minimum 32 characters (256 bits)
 *   jwt.expiration-ms — token lifetime in milliseconds (default 1 hour)
 *
 * Startup will fail with a clear error if the secret is missing or too short.
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private static final int MIN_SECRET_LENGTH = 32;

    private String secret;
    private long expirationMs = 3_600_000L; // 1 hour default

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    /**
     * Validates that the secret is present and long enough for HMAC-SHA256.
     * Called by JwtService at startup via @PostConstruct.
     *
     * @throws IllegalStateException if the secret is missing or too short
     */
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret is not configured. Set JWT_SECRET environment variable. " +
                    "Generate a strong secret with: openssl rand -hex 32");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "jwt.secret is too short (" + secret.length() + " characters). " +
                    "Minimum length is " + MIN_SECRET_LENGTH + " characters (256 bits).");
        }
    }
}
