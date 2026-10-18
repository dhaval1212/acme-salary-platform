package com.acme.compensation.auth;

import com.acme.compensation.auth.dto.LoginRequest;
import com.acme.compensation.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller slice tests for AuthController.
 * Spring Security filter chain is NOT loaded — tests HTTP contract only.
 * Integration tests (with full security) live in AuthSecurityIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService            jwtService;
    @Mock UserRepository        userRepository;

    @InjectMocks AuthController controller;

    MockMvc mockMvc;
    JsonMapper jsonMapper;

    private static final String LOGIN_URL = "/api/auth/login";

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .build();
    }

    private com.acme.compensation.auth.User mockUser() {
        com.acme.compensation.auth.User user = new com.acme.compensation.auth.User();
        // Use reflection to set ID (generated field)
        try {
            var f = com.acme.compensation.auth.User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(user, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        } catch (Exception e) { throw new RuntimeException(e); }
        user.setUsername("hr.manager");
        user.setPasswordHash("$2a$12$hash");
        user.setFullName("HR Manager");
        user.setRole("HR_MANAGER");
        user.setEnabled(true);
        return user;
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 with token and user profile on valid credentials")
        void returns200OnValidCredentials() throws Exception {
            UserDetails userDetails = User.builder().username("hr.manager")
                    .password("hashed").authorities("ROLE_HR_MANAGER").build();
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            com.acme.compensation.auth.User user = mockUser();

            given(authenticationManager.authenticate(any())).willReturn(auth);
            given(userRepository.findByUsername("hr.manager")).willReturn(Optional.of(user));
            given(jwtService.generateToken(any(), any())).willReturn("mock.jwt.token");
            given(jwtService.getExpiry()).willReturn(Instant.now().plusSeconds(3600));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new LoginRequest("hr.manager", "password"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.token").value("mock.jwt.token"))
                    .andExpect(jsonPath("$.data.user.username").value("hr.manager"))
                    .andExpect(jsonPath("$.data.user.role").value("HR_MANAGER"))
                    .andExpect(jsonPath("$.data.expiresAt").exists());
        }

        @Test
        @DisplayName("returns 200 via /api/v1/auth/login endpoint")
        void returns200OnV1Endpoint() throws Exception {
            UserDetails userDetails = User.builder().username("hr.manager")
                    .password("hashed").authorities("ROLE_HR_MANAGER").build();
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            com.acme.compensation.auth.User user = mockUser();

            given(authenticationManager.authenticate(any())).willReturn(auth);
            given(userRepository.findByUsername("hr.manager")).willReturn(Optional.of(user));
            given(jwtService.generateToken(any(), any())).willReturn("mock.jwt.token");
            given(jwtService.getExpiry()).willReturn(Instant.now().plusSeconds(3600));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new LoginRequest("hr.manager", "password"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.token").value("mock.jwt.token"));
        }

        @Test
        @DisplayName("never returns password hash in response")
        void neverReturnsPasswordHash() throws Exception {
            UserDetails userDetails = User.builder().username("hr.manager")
                    .password("hashed").authorities("ROLE_HR_MANAGER").build();
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            com.acme.compensation.auth.User user = mockUser();

            given(authenticationManager.authenticate(any())).willReturn(auth);
            given(userRepository.findByUsername("hr.manager")).willReturn(Optional.of(user));
            given(jwtService.generateToken(any(), any())).willReturn("mock.jwt.token");
            given(jwtService.getExpiry()).willReturn(Instant.now().plusSeconds(3600));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new LoginRequest("hr.manager", "password"))))
                    .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                    .andExpect(jsonPath("$.data.user.password").doesNotExist());
        }

        @Test
        @DisplayName("returns 401 with generic message on bad credentials")
        void returns401OnBadCredentials() throws Exception {
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("bad credentials"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new LoginRequest("hr.manager", "wrong"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errors[0].message").value("Invalid credentials"));
        }

        @Test
        @DisplayName("returns 401 with generic message for unknown username")
        void returns401ForUnknownUsername() throws Exception {
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("bad credentials"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new LoginRequest("unknown", "password"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errors[0].message").value("Invalid credentials"));
        }

        @Test
        @DisplayName("returns 401 with generic message for disabled account")
        void returns401ForDisabledAccount() throws Exception {
            given(authenticationManager.authenticate(any()))
                    .willThrow(new DisabledException("account disabled"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new LoginRequest("disabled.user", "password"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errors[0].message").value("Invalid credentials"));
        }

        @Test
        @DisplayName("returns 400 when username is blank")
        void returns400WhenUsernameBlank() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new LoginRequest("", "password"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field=='username')]").exists());
        }

        @Test
        @DisplayName("returns 400 when password is blank")
        void returns400WhenPasswordBlank() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new LoginRequest("hr.manager", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field=='password')]").exists());
        }

        @Test
        @DisplayName("error message does not differ between wrong password and unknown username")
        void genericErrorMessageForBothFailureCases() throws Exception {
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("bad"));

            // Wrong password
            var result1 = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(new LoginRequest("hr.manager", "wrong"))))
                    .andReturn().getResponse().getContentAsString();

            // Unknown user
            var result2 = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(new LoginRequest("ghost", "any"))))
                    .andReturn().getResponse().getContentAsString();

            // Both must contain the exact same error message
            org.assertj.core.api.Assertions.assertThat(result1).contains("Invalid credentials");
            org.assertj.core.api.Assertions.assertThat(result2).contains("Invalid credentials");
            org.assertj.core.api.Assertions.assertThat(result1).isEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("No registration endpoint")
    class NoRegistration {

        @Test
        @DisplayName("AuthController has no register mapping — only /login exists")
        void onlyLoginEndpointExists() throws Exception {
            // AuthController only maps POST /api/auth/login.
            // Attempting /api/auth/register with standaloneSetup produces a 404 at
            // the dispatcher level — verified here as a non-200 response, with full
            // 404 verification in AuthSecurityIntegrationTest.
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(result ->
                            org.assertj.core.api.Assertions.assertThat(
                                    result.getResponse().getStatus()).isNotEqualTo(200));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {

        @Test
        @DisplayName("returns 200 with success message on logout")
        void returns200OnLogout() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.message").value("Logged out successfully"));
        }

        @Test
        @DisplayName("returns 200 via /api/v1/auth/logout endpoint")
        void returns200OnV1Logout() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.message").value("Logged out successfully"));
        }
    }
}
