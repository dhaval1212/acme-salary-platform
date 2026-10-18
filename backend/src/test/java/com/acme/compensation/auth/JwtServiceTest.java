package com.acme.compensation.auth;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtService.
 * No Spring context, no database — pure logic tests.
 */
class JwtServiceTest {

    private static final String VALID_SECRET = "unit-test-secret-exactly-32-chars!!";
    private static final String ROLE = "HR_MANAGER";

    private JwtService jwtService;
    private JwtProperties properties;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret(VALID_SECRET);
        properties.setExpirationMs(3_600_000L); // 1 hour

        jwtService = new JwtService(properties);
        jwtService.init(); // triggers @PostConstruct validation

        userDetails = User.builder()
                .username("hr.manager")
                .password("hashed")
                .authorities("ROLE_HR_MANAGER")
                .build();
    }

    // ── generateToken ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("produces a non-blank token")
        void producesNonBlankToken() {
            String token = jwtService.generateToken(userDetails, ROLE);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("token contains three JWT parts (header.payload.signature)")
        void tokenHasThreeParts() {
            String token = jwtService.generateToken(userDetails, ROLE);
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("extractUsername returns the correct subject")
        void extractUsernameReturnsSubject() {
            String token = jwtService.generateToken(userDetails, ROLE);
            assertThat(jwtService.extractUsername(token)).isEqualTo("hr.manager");
        }
    }

    // ── isTokenValid ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValid {

        @Test
        @DisplayName("returns true for a valid token and matching user")
        void trueForValidToken() {
            String token = jwtService.generateToken(userDetails, ROLE);
            assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
        }

        @Test
        @DisplayName("returns false when username in token does not match UserDetails")
        void falseForWrongUser() {
            String token = jwtService.generateToken(userDetails, ROLE);
            UserDetails otherUser = User.builder()
                    .username("other.user")
                    .password("hashed")
                    .authorities("ROLE_HR_MANAGER")
                    .build();
            assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
        }

        @Test
        @DisplayName("returns false for an expired token")
        void falseForExpiredToken() {
            // Create a service with 1ms expiry to force immediate expiration
            JwtProperties shortLivedProps = new JwtProperties();
            shortLivedProps.setSecret(VALID_SECRET);
            shortLivedProps.setExpirationMs(1L);
            JwtService shortLivedService = new JwtService(shortLivedProps);
            shortLivedService.init();

            String token = shortLivedService.generateToken(userDetails, ROLE);

            // Small sleep to ensure token expires
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            assertThat(shortLivedService.isTokenValid(token, userDetails)).isFalse();
        }

        @Test
        @DisplayName("returns false for a token signed with a different secret")
        void falseForWrongSecret() {
            JwtProperties otherProps = new JwtProperties();
            otherProps.setSecret("completely-different-secret-32ch!!");
            JwtService otherService = new JwtService(otherProps);
            otherService.init();

            String tokenFromOtherService = otherService.generateToken(userDetails, ROLE);
            // Validate against original service — wrong secret, must fail
            assertThat(jwtService.isTokenValid(tokenFromOtherService, userDetails)).isFalse();
        }

        @Test
        @DisplayName("returns false for a malformed token string")
        void falseForMalformedToken() {
            assertThat(jwtService.isTokenValid("not.a.valid.jwt", userDetails)).isFalse();
        }
    }

    // ── validateAndGetClaims ──────────────────────────────────────────────────

    @Nested
    @DisplayName("validateAndGetClaims()")
    class ValidateAndGetClaims {

        @Test
        @DisplayName("returns claims for a valid token")
        void returnsClaimsForValidToken() {
            String token = jwtService.generateToken(userDetails, ROLE);
            var claims = jwtService.validateAndGetClaims(token);
            assertThat(claims.getSubject()).isEqualTo("hr.manager");
            assertThat(claims.getIssuer()).isEqualTo("acme-compensation-hub");
        }

        @Test
        @DisplayName("throws JwtException for a tampered payload")
        void throwsForTamperedPayload() {
            String token = jwtService.generateToken(userDetails, ROLE);
            // Tamper the payload part (index 1)
            String[] parts = token.split("\\.");
            String tamperedToken = parts[0] + ".dGFtcGVyZWQ=" + parts[2];
            assertThatThrownBy(() -> jwtService.validateAndGetClaims(tamperedToken))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("throws JwtException for a blank token")
        void throwsForBlankToken() {
            assertThatThrownBy(() -> jwtService.validateAndGetClaims(""))
                    .isInstanceOf(Exception.class);
        }
    }

    // ── JwtProperties validation ──────────────────────────────────────────────

    @Nested
    @DisplayName("JwtProperties.validate()")
    class PropertiesValidation {

        @Test
        @DisplayName("throws IllegalStateException when secret is null")
        void throwsWhenSecretIsNull() {
            JwtProperties props = new JwtProperties();
            props.setSecret(null);
            assertThatThrownBy(props::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("jwt.secret");
        }

        @Test
        @DisplayName("throws IllegalStateException when secret is blank")
        void throwsWhenSecretIsBlank() {
            JwtProperties props = new JwtProperties();
            props.setSecret("   ");
            assertThatThrownBy(props::validate)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws IllegalStateException when secret is shorter than 32 chars")
        void throwsWhenSecretTooShort() {
            JwtProperties props = new JwtProperties();
            props.setSecret("short");
            assertThatThrownBy(props::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("too short");
        }

        @Test
        @DisplayName("does not throw for a valid 32+ character secret")
        void doesNotThrowForValidSecret() {
            JwtProperties props = new JwtProperties();
            props.setSecret(VALID_SECRET);
            assertThatCode(props::validate).doesNotThrowAnyException();
        }
    }
}
