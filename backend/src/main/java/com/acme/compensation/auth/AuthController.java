package com.acme.compensation.auth;

import com.acme.compensation.auth.dto.AuthUserProfile;
import com.acme.compensation.auth.dto.LoginRequest;
import com.acme.compensation.auth.dto.LoginResponse;
import com.acme.compensation.common.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Authentication endpoints.
 * POST /api/auth/login  — public, no token required
 *
 * Security notes:
 *  - Invalid credentials produce a generic "Invalid credentials" message
 *    that does not reveal whether the username or password was wrong.
 *  - Passwords and password hashes are never logged or returned.
 *  - No registration endpoint exists on this controller or anywhere else.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    // Generic error message — never reveal whether username or password was incorrect
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService            = jwtService;
        this.userRepository        = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            UserDetails userDetails = (UserDetails) auth.getPrincipal();

            // Fetch the full User entity to include role and fullName in the response
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

            String token = jwtService.generateToken(userDetails, user.getRole());

            LoginResponse response = new LoginResponse(
                    token,
                    jwtService.getExpiry(),
                    new AuthUserProfile(user.getId(), user.getUsername(), user.getFullName(), user.getRole())
            );

            // Log at INFO — username only, never password or token
            log.info("Successful login for user: {}", user.getUsername());
            return ResponseEntity.ok(ApiResponse.ok(response));

        } catch (DisabledException ex) {
            // Account is disabled — return same generic message to prevent enumeration
            log.warn("Login attempt for disabled account: {}", request.username());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("global", INVALID_CREDENTIALS_MESSAGE));

        } catch (BadCredentialsException ex) {
            // Wrong password or unknown username — generic message
            log.warn("Failed login attempt for username: {}", request.username());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("global", INVALID_CREDENTIALS_MESSAGE));
        }
    }
}
