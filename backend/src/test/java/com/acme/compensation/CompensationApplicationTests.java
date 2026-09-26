package com.acme.compensation;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test — verifies the full Spring context loads against a real database.
 *
 * Requires Docker to be running. Tagged as "integration" so it can be excluded
 * from fast unit test runs:
 *   Unit tests only:       ./mvnw test -DexcludedGroups=integration
 *   Integration tests too: ./mvnw test  (or ./mvnw test -Dgroups=integration)
 *
 * What this test proves:
 *   - All Liquibase migrations run without error
 *   - All Spring beans wire correctly
 *   - JPA entity mappings are valid against the real schema
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CompensationApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void contextLoads() {
        // Passing means: migrations applied, beans wired, schema valid.
    }
}
