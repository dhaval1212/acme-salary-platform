package com.acme.compensation.auth;

import com.acme.compensation.auth.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full security integration tests using a real Spring context + Testcontainers.
 * Tests the complete request/response cycle including Spring Security filter chain.
 *
 * Uses Spring 7's RestClient (Spring Boot 4.x removed TestRestTemplate).
 *
 * Tagged "integration" so they only run when Docker is available:
 *   ./mvnw test -Dgroups=integration
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuthSecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserRepository userRepository;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (req, resp) -> { })
                .build();
    }

    private String login(String username, String password) {
        LoginRequest request = new LoginRequest(username, password);
        ResponseEntity<String> response = client().post()
                .uri("/api/auth/login")
                .body(request)
                .retrieve()
                .toEntity(String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).contains("token");
        int start = body.indexOf("\"token\":\"") + 9;
        int end   = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    // ── Login success ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Login — success")
    class LoginSuccess {

        @Test
        @DisplayName("valid credentials return 200 with token and safe user profile")
        void validCredentialsReturn200() {
            LoginRequest request = new LoginRequest("hr.manager", "changeme123");
            ResponseEntity<String> response = client().post()
                    .uri("/api/auth/login")
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            String body = response.getBody();
            assertThat(body).contains("token");
            assertThat(body).contains("expiresAt");
            assertThat(body).contains("hr.manager");
            assertThat(body).contains("HR_MANAGER");
        }

        @Test
        @DisplayName("response never contains password hash")
        void responseDoesNotContainPasswordHash() {
            LoginRequest request = new LoginRequest("hr.manager", "changeme123");
            ResponseEntity<String> response = client().post()
                    .uri("/api/auth/login")
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);

            String body = response.getBody();
            assertThat(body).doesNotContain("$2a$");
            assertThat(body).doesNotContain("passwordHash");
            assertThat(body).doesNotContain("password_hash");
        }

        @Test
        @DisplayName("passwords are stored as BCrypt hashes, not plaintext")
        void passwordsStoredAsBcryptHashes() {
            User user = userRepository.findByUsername("hr.manager")
                    .orElseThrow(() -> new AssertionError("Test user not found"));

            assertThat(user.getPasswordHash()).startsWith("$2a$");
            assertThat(user.getPasswordHash()).isNotEqualTo("changeme123");
            assertThat(passwordEncoder.matches("changeme123", user.getPasswordHash())).isTrue();
        }
    }

    // ── Login failure ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Login — failure")
    class LoginFailure {

        @Test
        @DisplayName("wrong password returns 401 with generic message")
        void wrongPasswordReturns401() {
            LoginRequest request = new LoginRequest("hr.manager", "wrong-password");
            ResponseEntity<String> response = client().post()
                    .uri("/api/auth/login")
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).contains("Invalid credentials");
        }

        @Test
        @DisplayName("unknown username returns 401 with generic message")
        void unknownUsernameReturns401() {
            LoginRequest request = new LoginRequest("ghost.user", "any-password");
            ResponseEntity<String> response = client().post()
                    .uri("/api/auth/login")
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).contains("Invalid credentials");
        }

        @Test
        @DisplayName("disabled account returns 401 with the same generic message")
        void disabledAccountReturns401() {
            LoginRequest request = new LoginRequest("disabled.user", "changeme123");
            ResponseEntity<String> response = client().post()
                    .uri("/api/auth/login")
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).contains("Invalid credentials");
        }

        @Test
        @DisplayName("error message is identical for wrong password and unknown user")
        void sameErrorMessagePreventEnumeration() {
            LoginRequest wrongPassword = new LoginRequest("hr.manager", "wrong");
            LoginRequest unknownUser   = new LoginRequest("ghost", "password");

            String body1 = client().post().uri("/api/auth/login").body(wrongPassword).retrieve().toEntity(String.class).getBody();
            String body2 = client().post().uri("/api/auth/login").body(unknownUser).retrieve().toEntity(String.class).getBody();

            assertThat(body1).isEqualTo(body2);
        }
    }

    // ── Protected endpoints ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Protected endpoints")
    class ProtectedEndpoints {

        @Test
        @DisplayName("request without token to /api/v1/employees returns 401")
        void noTokenReturns401() {
            ResponseEntity<String> response = client().get()
                    .uri("/api/v1/employees")
                    .retrieve()
                    .toEntity(String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).contains("Authentication required");
        }

        @Test
        @DisplayName("valid token grants access to /api/v1/employees")
        void validTokenGrantsAccess() {
            String token = login("hr.manager", "changeme123");
            ResponseEntity<String> response = client().get()
                    .uri("/api/v1/employees")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toEntity(String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("expired/malformed token returns 401")
        void malformedTokenReturns401() {
            ResponseEntity<String> response = client().get()
                    .uri("/api/v1/employees")
                    .header("Authorization", "Bearer this.is.not.a.valid.jwt")
                    .retrieve()
                    .toEntity(String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("tampered token (modified payload) returns 401")
        void tamperedTokenReturns401() {
            String token = login("hr.manager", "changeme123");
            String[] parts = token.split("\\.");
            String tampered = parts[0] + ".dGFtcGVyZWQ=" + "." + parts[2];

            ResponseEntity<String> response = client().get()
                    .uri("/api/v1/employees")
                    .header("Authorization", "Bearer " + tampered)
                    .retrieve()
                    .toEntity(String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── No registration endpoint ──────────────────────────────────────────────

    @Nested
    @DisplayName("No public registration")
    class NoRegistration {

        @Test
        @DisplayName("POST /api/auth/register returns 401/404 — no registration endpoint")
        void noRegistrationEndpoint() {
            ResponseEntity<String> response = client().post()
                    .uri("/api/auth/register")
                    .body("{}")
                    .retrieve()
                    .toEntity(String.class);

            assertThat(response.getStatusCode().value()).isIn(401, 403, 404);
        }

        @Test
        @DisplayName("POST /api/v1/users returns 401/404 — no user creation API")
        void noUserCreationApi() {
            ResponseEntity<String> response = client().post()
                    .uri("/api/v1/users")
                    .body("{}")
                    .retrieve()
                    .toEntity(String.class);

            assertThat(response.getStatusCode().value()).isIn(401, 403, 404);
        }
    }

    // ── Health check remains public ───────────────────────────────────────────

    @Nested
    @DisplayName("Public endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("GET /actuator/health is accessible without authentication")
        void healthEndpointIsPublic() {
            ResponseEntity<String> response = client().get()
                    .uri("/actuator/health")
                    .retrieve()
                    .toEntity(String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
