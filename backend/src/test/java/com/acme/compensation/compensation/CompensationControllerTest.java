package com.acme.compensation.compensation;

import com.acme.compensation.common.ConflictException;
import com.acme.compensation.common.GlobalExceptionHandler;
import com.acme.compensation.common.ResourceNotFoundException;
import com.acme.compensation.compensation.dto.CompensationRequest;
import com.acme.compensation.compensation.dto.CompensationResponse;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.acme.compensation.TestFixtures.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for CompensationController.
 * Uses standaloneSetup — no Spring context, no database.
 * CompensationService is mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
class CompensationControllerTest {

    @Mock
    CompensationService service;

    @InjectMocks
    CompensationController controller;

    MockMvc mockMvc;
    JsonMapper jsonMapper;

    private static final String BASE = "/api/v1/employees/" + EMPLOYEE_ID + "/compensation";

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .build();
    }

    private CompensationResponse sampleResponse() {
        return new CompensationResponse(
                COMP_ID, EMPLOYEE_ID,
                new BigDecimal("90000.00"), "USD",
                LocalDate.of(2026, 1, 1),
                "hr-manager@acme.com",
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    // ── GET /compensation ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/employees/{id}/compensation")
    class GetCurrent {

        @Test
        @DisplayName("returns 200 with current compensation")
        void returns200WithCurrentComp() throws Exception {
            given(service.getCurrent(EMPLOYEE_ID)).willReturn(sampleResponse());

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.amount").value(90000.00))
                    .andExpect(jsonPath("$.data.currency").value("USD"))
                    .andExpect(jsonPath("$.data.effectiveDate").value("2026-01-01"))
                    .andExpect(jsonPath("$.data.employeeId").value(EMPLOYEE_ID.toString()));
        }

        @Test
        @DisplayName("returns 404 when employee does not exist")
        void returns404WhenEmployeeNotFound() throws Exception {
            given(service.getCurrent(EMPLOYEE_ID))
                    .willThrow(new ResourceNotFoundException("Employee", EMPLOYEE_ID));

            mockMvc.perform(get(BASE))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 404 when no compensation record exists yet")
        void returns404WhenNoRecord() throws Exception {
            given(service.getCurrent(EMPLOYEE_ID))
                    .willThrow(new ResourceNotFoundException("Compensation record", EMPLOYEE_ID));

            mockMvc.perform(get(BASE))
                    .andExpect(status().isNotFound());
        }
    }

    // ── GET /compensation/history ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/employees/{id}/compensation/history")
    class GetHistory {

        @Test
        @DisplayName("returns 200 with full history list")
        void returns200WithHistory() throws Exception {
            CompensationResponse older = new CompensationResponse(
                    COMP_ID, EMPLOYEE_ID,
                    new BigDecimal("80000.00"), "USD",
                    LocalDate.of(2025, 1, 1),
                    "hr-manager@acme.com",
                    Instant.parse("2025-01-01T00:00:00Z")
            );
            given(service.getHistory(EMPLOYEE_ID)).willReturn(List.of(sampleResponse(), older));

            mockMvc.perform(get(BASE + "/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].effectiveDate").value("2026-01-01"))
                    .andExpect(jsonPath("$.data[1].effectiveDate").value("2025-01-01"));
        }

        @Test
        @DisplayName("returns 200 with empty array when no history exists")
        void returns200WhenNoHistory() throws Exception {
            given(service.getHistory(EMPLOYEE_ID)).willReturn(List.of());

            mockMvc.perform(get(BASE + "/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    // ── POST /compensation ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/employees/{id}/compensation")
    class RecordCompensation {

        @Test
        @DisplayName("returns 201 with recorded entry when request is valid")
        void returns201WhenValid() throws Exception {
            given(service.record(eq(EMPLOYEE_ID), any())).willReturn(sampleResponse());

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(validCompensationRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.amount").value(90000.00))
                    .andExpect(jsonPath("$.data.currency").value("USD"));
        }

        @Test
        @DisplayName("returns 400 when amount is null")
        void returns400WhenAmountNull() throws Exception {
            CompensationRequest bad = new CompensationRequest(
                    null, "USD", LocalDate.of(2026, 1, 1), "hr@acme.com");

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field=='amount')]").exists());
        }

        @Test
        @DisplayName("returns 400 when amount is negative")
        void returns400WhenAmountNegative() throws Exception {
            CompensationRequest bad = new CompensationRequest(
                    new BigDecimal("-1.00"), "USD", LocalDate.of(2026, 1, 1), "hr@acme.com");

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field=='amount')]").exists());
        }

        @Test
        @DisplayName("returns 400 when currency is not a valid 3-letter uppercase code")
        void returns400WhenCurrencyInvalid() throws Exception {
            CompensationRequest bad = new CompensationRequest(
                    new BigDecimal("90000.00"), "dollars", LocalDate.of(2026, 1, 1), "hr@acme.com");

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field=='currency')]").exists());
        }

        @Test
        @DisplayName("returns 400 when effectiveDate is null")
        void returns400WhenEffectiveDateNull() throws Exception {
            CompensationRequest bad = new CompensationRequest(
                    new BigDecimal("90000.00"), "USD", null, "hr@acme.com");

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field=='effectiveDate')]").exists());
        }

        @Test
        @DisplayName("returns 400 when changedBy is blank")
        void returns400WhenChangedByBlank() throws Exception {
            CompensationRequest bad = new CompensationRequest(
                    new BigDecimal("90000.00"), "USD", LocalDate.of(2026, 1, 1), "");

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field=='changedBy')]").exists());
        }

        @Test
        @DisplayName("returns 409 when a record already exists for this employee on this date")
        void returns409OnDuplicateDate() throws Exception {
            given(service.record(eq(EMPLOYEE_ID), any()))
                    .willThrow(new ConflictException(
                            "A compensation record for this employee already exists on 2026-01-01"));

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(validCompensationRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errors[0].message", containsString("2026-01-01")));
        }

        @Test
        @DisplayName("returns 404 when employee does not exist")
        void returns404WhenEmployeeNotFound() throws Exception {
            given(service.record(eq(EMPLOYEE_ID), any()))
                    .willThrow(new ResourceNotFoundException("Employee", EMPLOYEE_ID));

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(validCompensationRequest())))
                    .andExpect(status().isNotFound());
        }
    }
}
