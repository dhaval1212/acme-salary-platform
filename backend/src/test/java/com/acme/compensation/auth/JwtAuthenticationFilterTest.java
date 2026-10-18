package com.acme.compensation.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("login endpoint is bypassed from JWT authentication filter")
    void loginEndpoint_isBypassedFromFilter() throws ServletException, IOException {
        request.setServletPath("/api/auth/login");
        request.addHeader("Authorization", "Bearer some-token");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("v1 login endpoint is bypassed from JWT authentication filter")
    void v1LoginEndpoint_isBypassedFromFilter() throws ServletException, IOException {
        request.setServletPath("/api/v1/auth/login");
        request.addHeader("Authorization", "Bearer some-token");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("null Authorization header continues chain without authentication")
    void nullAuthHeader_continuesChain() throws ServletException, IOException {
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("non-Bearer Authorization header continues chain without authentication")
    void nonBearerAuthHeader_continuesChain() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("valid Bearer token authenticates user and sets SecurityContext")
    void validToken_authenticatesUser() throws ServletException, IOException {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        UserDetails userDetails = User.builder()
                .username("hr.manager")
                .password("hash")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_HR_MANAGER")))
                .build();

        when(jwtService.extractUsername(token)).thenReturn("hr.manager");
        when(userDetailsService.loadUserByUsername("hr.manager")).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("hr.manager");
        assertThat(auth.getAuthorities()).hasSize(1);
    }

    @Test
    @DisplayName("already authenticated request does not re-authenticate")
    void alreadyAuthenticated_doesNotReauthenticate() throws ServletException, IOException {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtService.extractUsername(token)).thenReturn("hr.manager");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existingAuth);
    }

    @Test
    @DisplayName("null username extracted from token does not authenticate")
    void nullUsername_doesNotAuthenticate() throws ServletException, IOException {
        String token = "token.with.null.user";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.extractUsername(token)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("invalid token does not authenticate")
    void invalidToken_doesNotAuthenticate() throws ServletException, IOException {
        String token = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        UserDetails userDetails = User.builder()
                .username("hr.manager")
                .password("hash")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_HR_MANAGER")))
                .build();

        when(jwtService.extractUsername(token)).thenReturn("hr.manager");
        when(userDetailsService.loadUserByUsername("hr.manager")).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("JwtException leaves SecurityContext empty and continues filter chain")
    void jwtException_leavesContextEmpty() throws ServletException, IOException {
        String token = "corrupted.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.extractUsername(token)).thenThrow(new JwtException("Signature invalid") { });

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("unexpected exception leaves SecurityContext empty and continues filter chain")
    void unexpectedException_leavesContextEmpty() throws ServletException, IOException {
        String token = "any.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.extractUsername(token)).thenThrow(new RuntimeException("Unexpected error"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
