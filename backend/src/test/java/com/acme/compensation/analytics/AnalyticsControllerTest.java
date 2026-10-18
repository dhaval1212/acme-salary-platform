package com.acme.compensation.analytics;

import com.acme.compensation.analytics.dto.DepartmentStats;
import com.acme.compensation.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for AnalyticsController.
 * Uses standaloneSetup — no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    AnalyticsService service;

    @InjectMocks
    AnalyticsController controller;

    MockMvc mockMvc;

    private static final String BASE = "/api/v1/analytics/departments";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(JsonMapper.builder().build()))
                .build();
    }

    private DepartmentStats engineeringStats() {
        return new DepartmentStats("Engineering", "United States", "USD", 10L,
                new BigDecimal("100000.00"), new BigDecimal("98000.00"),
                new BigDecimal("75000.00"), new BigDecimal("130000.00"));
    }

    // ── GET /analytics/departments ────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/analytics/departments")
    class ByDepartment {

        @Test
        @DisplayName("returns 200 with stats when data exists")
        void returns200WithStats() throws Exception {
            given(service.getDepartmentStats(null, null)).willReturn(List.of(engineeringStats()));

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].department").value("Engineering"))
                    .andExpect(jsonPath("$.data[0].headcount").value(10))
                    .andExpect(jsonPath("$.data[0].avgSalary").value(100000.00))
                    .andExpect(jsonPath("$.data[0].medianSalary").value(98000.00))
                    .andExpect(jsonPath("$.data[0].minSalary").value(75000.00))
                    .andExpect(jsonPath("$.data[0].maxSalary").value(130000.00));
        }

        @Test
        @DisplayName("returns 200 with empty array when no data")
        void returns200WhenEmpty() throws Exception {
            given(service.getDepartmentStats(null, null)).willReturn(List.of());

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("passes department filter to service")
        void passesDepartmentFilter() throws Exception {
            given(service.getDepartmentStats("Engineering", null))
                    .willReturn(List.of(engineeringStats()));

            mockMvc.perform(get(BASE).param("department", "Engineering"))
                    .andExpect(status().isOk());

            then(service).should().getDepartmentStats("Engineering", null);
        }

        @Test
        @DisplayName("passes country filter to service")
        void passesCountryFilter() throws Exception {
            given(service.getDepartmentStats(null, "US")).willReturn(List.of());

            mockMvc.perform(get(BASE).param("country", "US"))
                    .andExpect(status().isOk());

            then(service).should().getDepartmentStats(null, "US");
        }

        @Test
        @DisplayName("passes both department and country filters to service")
        void passesBothFilters() throws Exception {
            given(service.getDepartmentStats("Engineering", "US"))
                    .willReturn(List.of(engineeringStats()));

            mockMvc.perform(get(BASE)
                            .param("department", "Engineering")
                            .param("country", "US"))
                    .andExpect(status().isOk());

            then(service).should().getDepartmentStats("Engineering", "US");
        }

        @Test
        @DisplayName("returns stats for multiple departments")
        void returnsMultipleDepartments() throws Exception {
            DepartmentStats hrStats = new DepartmentStats("HR", "United Kingdom", "GBP", 3L,
                    new BigDecimal("65000.00"), new BigDecimal("63000.00"),
                    new BigDecimal("55000.00"), new BigDecimal("70000.00"));
            given(service.getDepartmentStats(null, null))
                    .willReturn(List.of(engineeringStats(), hrStats));

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].department").value("Engineering"))
                    .andExpect(jsonPath("$.data[0].country").value("United States"))
                    .andExpect(jsonPath("$.data[0].currency").value("USD"))
                    .andExpect(jsonPath("$.data[1].department").value("HR"))
                    .andExpect(jsonPath("$.data[1].country").value("United Kingdom"))
                    .andExpect(jsonPath("$.data[1].currency").value("GBP"));
        }

        @Test
        @DisplayName("errors field is absent on successful response")
        void errorsAbsentOnSuccess() throws Exception {
            given(service.getDepartmentStats(any(), any())).willReturn(List.of());

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").doesNotExist());
        }
    }
}
