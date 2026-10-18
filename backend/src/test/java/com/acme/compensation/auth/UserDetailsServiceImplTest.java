package com.acme.compensation.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl Unit Tests")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserDetailsServiceImpl(userRepository);
    }

    @Test
    @DisplayName("loadUserByUsername returns UserDetails when enabled user exists")
    void loadUserByUsername_enabledUser_returnsUserDetails() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("hr.manager");
        user.setPasswordHash("$2a$12$hash");
        user.setRole("HR_MANAGER");
        user.setEnabled(true);

        when(userRepository.findByUsername("hr.manager")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("hr.manager");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("hr.manager");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$12$hash");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_HR_MANAGER");
    }

    @Test
    @DisplayName("loadUserByUsername returns disabled UserDetails when user is disabled")
    void loadUserByUsername_disabledUser_returnsDisabledUserDetails() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("disabled.user");
        user.setPasswordHash("$2a$12$hash");
        user.setRole("HR_MANAGER");
        user.setEnabled(false);

        when(userRepository.findByUsername("disabled.user")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("disabled.user");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("disabled.user");
        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException when user is not found")
    void loadUserByUsername_userNotFound_throwsException() {
        when(userRepository.findByUsername("unknown.user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown.user"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Authentication failed");
    }
}
