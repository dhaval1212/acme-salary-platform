package com.acme.compensation.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User Entity Unit Tests")
class UserTest {

    @Test
    @DisplayName("getters and setters work as expected")
    void gettersAndSetters() {
        User user = new User();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        user.setId(id);
        user.setUsername("test.user");
        user.setPasswordHash("$2a$12$hash");
        user.setFullName("Test User");
        user.setRole("HR_MANAGER");
        user.setEnabled(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getUsername()).isEqualTo("test.user");
        assertThat(user.getPasswordHash()).isEqualTo("$2a$12$hash");
        assertThat(user.getFullName()).isEqualTo("Test User");
        assertThat(user.getRole()).isEqualTo("HR_MANAGER");
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("default values are correctly initialized")
    void defaultValues() {
        User user = new User();

        assertThat(user.getRole()).isEqualTo("HR_MANAGER");
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("lifecycle hooks onCreate and onUpdate populate timestamps")
    void lifecycleHooks() {
        User user = new User();

        user.onCreate();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();

        Instant beforeUpdate = user.getUpdatedAt();
        user.onUpdate();
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }
}
