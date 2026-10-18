package com.acme.compensation.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Generates and validates JWT access tokens.
 *
 * Security properties enforced:
 *  - HMAC-SHA256 signature (HS256); unsigned ("alg:none") tokens are rejected.
 *  - Issuer claim ("acme-compensation-hub") validated on every parse.
 *  - Expiry validated on every parse; expired tokens produce a 401.
 *  - Startup fails if the signing secret is missing or too short.
 *  - Signing secret is never logged.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String ISSUER = "acme-compensation-hub";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;
    private SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        // Fail fast if secret is misconfigured — prevents running without security
        properties.validate();
        signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        log.info("JWT service initialised (expiry={}ms)", properties.getExpirationMs());
    }

    /**
     * Generates a signed JWT for the given user.
     *
     * @param userDetails the authenticated user
     * @param role        the user's application role (e.g. "HR_MANAGER")
     * @return signed compact JWT string
     */
    public String generateToken(UserDetails userDetails, String role) {
        Instant now    = Instant.now();
        Instant expiry = now.plusMillis(properties.getExpirationMs());

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuer(ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claims(Map.of(CLAIM_ROLE, role))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Returns the token expiry instant for use in the login response.
     */
    public Instant getExpiry() {
        return Instant.now().plusMillis(properties.getExpirationMs());
    }

    /**
     * Validates the token and returns its claims.
     * Throws JwtException (a RuntimeException subtype) on any failure.
     * The caller maps this to a 401 — the specific failure reason is logged
     * at DEBUG level but never returned to the client.
     */
    public Claims validateAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Convenience method: extract username from a validated token.
     * Callers should catch JwtException and treat it as unauthenticated.
     */
    public String extractUsername(String token) {
        return validateAndGetClaims(token).getSubject();
    }

    /**
     * Returns true only if the token is valid, not expired, and signed correctly.
     * All failures are treated identically to prevent information leakage.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername());
        } catch (ExpiredJwtException ex) {
            log.debug("JWT expired for subject: {}", ex.getClaims().getSubject());
            return false;
        } catch (JwtException ex) {
            log.debug("JWT validation failed: {}", ex.getClass().getSimpleName());
            return false;
        }
    }
}
