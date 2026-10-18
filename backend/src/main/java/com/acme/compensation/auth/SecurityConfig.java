package com.acme.compensation.auth;

import com.acme.compensation.common.ApiError;
import com.acme.compensation.common.ApiResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring Security configuration.
 *
 * Public endpoints:
 *   POST /api/auth/login
 *   GET  /actuator/health
 *   GET  /actuator/info
 *
 * All other /api/** endpoints require ROLE_HR_MANAGER.
 *
 * CSRF: Disabled — this API uses stateless Bearer-token authentication.
 * CSRF protection is only relevant for cookie-based session authentication.
 * Bearer tokens are not sent automatically by browsers, so CSRF attacks
 * do not apply here.
 *
 * CORS: Narrowly configured for the actual frontend origin(s) defined via
 * the CORS_ALLOWED_ORIGINS environment variable. Wildcard origins are not
 * permitted when credentials are involved.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          ObjectMapper objectMapper) {
        this.userDetailsService     = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper            = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── CSRF ───────────────────────────────────────────────────────
            // Disabled: stateless JWT API, Bearer tokens not auto-sent by browsers
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS ───────────────────────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── Session ────────────────────────────────────────────────────
            // Stateless: no server-side session, every request authenticated via JWT
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Authorization ──────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                    // Public: login endpoint
                    .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/v1/auth/login").permitAll()
                    // Public: health check
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    // Everything else requires authentication
                    .anyRequest().hasRole("HR_MANAGER")
            )

            // ── JWT filter ─────────────────────────────────────────────────
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // ── 401 handler — consistent ApiResponse envelope ──────────────
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(401);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                        ApiResponse<Void> body = ApiResponse.error(
                                List.of(ApiError.global("Authentication required")));
                        response.getWriter().write(objectMapper.writeValueAsString(body));
                    })
                    // ── 403 handler ────────────────────────────────────────
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setStatus(403);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                        ApiResponse<Void> body = ApiResponse.error(
                                List.of(ApiError.global("Access denied")));
                        response.getWriter().write(objectMapper.writeValueAsString(body));
                    })
            );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Parse comma-separated list of allowed origins from environment variable
        List<String> origins = List.of(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
