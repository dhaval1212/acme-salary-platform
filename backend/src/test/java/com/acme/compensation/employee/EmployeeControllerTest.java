package com.acme.compensation.employee;

import com.acme.compensation.common.ConflictException;
import com.acme.compensation.common.GlobalExceptionHandler;
import com.acme.compensation.common.ResourceNotFoundException;
import com.acme.compensation.employee.dto.EmployeeRequest;
import com.acme.compensation.employee.dto.EmployeeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static com.acme.compensation.TestFixtures.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for EmployeeController.
 * Uses standaloneSetup — no Spring context, no database.
 * EmployeeService is mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    @Mock
    EmployeeService service;

    @InjectMocks
    EmployeeController controller;

    MockMvc mockMvc;
    JsonMapper jsonMapper;

    private static final String BASE = "/api/v1/employees";

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .build();
    }

    private EmployeeResponse sampleResponse() {
        return new EmployeeResponse(
                EMPLOYEE_ID, "Alice Smith", "alice@acme.com",
                "Engineering", "Software Engineer", "US",
                EmployeeStatus.ACTIVE,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z")
        );
    }

    // ── GET /employees ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/employees")
    class ListEmployees {

        @Test
        @DisplayName("returns 200 with data and meta when employees exist")
        void returns200WithPagedData() throws Exception {
            given(service.list(any(), any(), any(), any(), org.mockito.ArgumentMatchers.any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(sampleResponse())));

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].email").value("alice@acme.com"))
                    .andExpect(jsonPath("$.meta.page").value(0))
                    .andExpect(jsonPath("$.meta.totalElements").value(1));
        }

        @Test
        @DisplayName("caps pageSize at 100")
        void capsPageSizeAt100() throws Exception {
            given(service.list(any(), any(), any(), any(), org.mockito.ArgumentMatchers.any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(BASE).param("pageSize", "500"))
                    .andExpect(status().isOk());

            then(service).should().list(any(), any(), any(), any(),
                    argThat(p -> p.getPageSize() == 100));
        }

        @Test
        @DisplayName("response does not contain salary fields")
        void responseDoesNotContainSalary() throws Exception {
            given(service.list(any(), any(), any(), any(), org.mockito.ArgumentMatchers.any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(sampleResponse())));

            mockMvc.perform(get(BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].amount").doesNotExist())
                    .andExpect(jsonPath("$.data[0].salary").doesNotExist())
                    .andExpect(jsonPath("$.data[0].currency").doesNotExist());
        }
    }

    // ── GET /employees/{id} ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/employees/{id}")
    class GetById {

        @Test
        @DisplayName("returns 200 with employee data when found")
        void returns200WhenFound() throws Exception {
            given(service.getById(EMPLOYEE_ID)).willReturn(sampleResponse());

            mockMvc.perform(get(BASE + "/" + EMPLOYEE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(EMPLOYEE_ID.toString()))
                    .andExpect(jsonPath("$.data.fullName").value("Alice Smith"));
        }

        @Test
        @DisplayName("returns 404 when employee does not exist")
        void returns404WhenNotFound() throws Exception {
            given(service.getById(UNKNOWN_ID))
                    .willThrow(new ResourceNotFoundException("Employee", UNKNOWN_ID));

            mockMvc.perform(get(BASE + "/" + UNKNOWN_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].field").value("global"));
        }
    }

    // ── POST /employees ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/employees")
    class CreateEmployee {

        @Test
        @DisplayName("returns 201 with created employee when request is valid")
        void returns201WhenValid() throws Exception {
            given(service.create(any())).willReturn(sampleResponse());

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(validEmployeeRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.email").value("alice@acme.com"))
                    .andExpect(header().string("Location", "/api/v1/employees/" + EMPLOYEE_ID));
        }

        @Test
        @DisplayName("returns 400 when fullName is blank")
        void returns400WhenFullNameBlank() throws Exception {
            EmployeeRequest bad = new EmployeeRequest("", "alice@acme.com", "Eng", "SWE", "US");

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field=='fullName')]").exists());
        }

        @Test
        @DisplayName("returns 400 when email is malformed")
        void returns400WhenEmailInvalid() throws Exception {
            EmployeeRequest bad = new EmployeeRequest("Alice", "not-an-email", "Eng", "SWE", "US");

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field=='email')]").exists());
        }

        @Test
        @DisplayName("returns 400 when multiple required fields are missing")
        void returns400WhenMultipleFieldsMissing() throws Exception {
            EmployeeRequest bad = new EmployeeRequest("", "", "", "", "");

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.length()", greaterThan(1)));
        }

        @Test
        @DisplayName("returns 409 when email already exists")
        void returns409OnDuplicateEmail() throws Exception {
            given(service.create(any()))
                    .willThrow(new ConflictException("An employee with email alice@acme.com already exists"));

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(validEmployeeRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errors[0].message", containsString("alice@acme.com")));
        }
    }

    // ── PUT /employees/{id} ───────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/v1/employees/{id}")
    class UpdateEmployee {

        @Test
        @DisplayName("returns 200 with updated employee when valid")
        void returns200WhenValid() throws Exception {
            given(service.update(eq(EMPLOYEE_ID), any())).willReturn(sampleResponse());

            mockMvc.perform(put(BASE + "/" + EMPLOYEE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(validEmployeeRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(EMPLOYEE_ID.toString()));
        }

        @Test
        @DisplayName("returns 404 when employee does not exist")
        void returns404WhenNotFound() throws Exception {
            given(service.update(eq(UNKNOWN_ID), any()))
                    .willThrow(new ResourceNotFoundException("Employee", UNKNOWN_ID));

            mockMvc.perform(put(BASE + "/" + UNKNOWN_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(validEmployeeRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // ── PATCH /employees/{id}/deactivate ──────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/employees/{id}/deactivate")
    class DeactivateEmployee {

        @Test
        @DisplayName("returns 200 with INACTIVE status when deactivation succeeds")
        void returns200WithInactiveStatus() throws Exception {
            EmployeeResponse inactive = new EmployeeResponse(
                    EMPLOYEE_ID, "Alice Smith", "alice@acme.com",
                    "Engineering", "Software Engineer", "US",
                    EmployeeStatus.INACTIVE,
                    Instant.parse("2025-01-01T00:00:00Z"),
                    Instant.parse("2026-01-01T00:00:00Z")
            );
            given(service.deactivate(EMPLOYEE_ID)).willReturn(inactive);

            mockMvc.perform(patch(BASE + "/" + EMPLOYEE_ID + "/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("INACTIVE"));
        }

        @Test
        @DisplayName("returns 404 when employee does not exist")
        void returns404WhenNotFound() throws Exception {
            given(service.deactivate(UNKNOWN_ID))
                    .willThrow(new ResourceNotFoundException("Employee", UNKNOWN_ID));

            mockMvc.perform(patch(BASE + "/" + UNKNOWN_ID + "/deactivate"))
                    .andExpect(status().isNotFound());
        }
    }
}
