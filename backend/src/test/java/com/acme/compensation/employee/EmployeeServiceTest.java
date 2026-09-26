package com.acme.compensation.employee;

import com.acme.compensation.TestFixtures;
import com.acme.compensation.common.ConflictException;
import com.acme.compensation.common.ResourceNotFoundException;
import com.acme.compensation.employee.dto.EmployeeRequest;
import com.acme.compensation.employee.dto.EmployeeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.acme.compensation.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for EmployeeService.
 * Repository is mocked — no database or Spring context required.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    EmployeeRepository repository;

    @InjectMocks
    EmployeeService service;

    Employee employee;

    @BeforeEach
    void setUp() {
        employee = activeEmployee();
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("list()")
    class List_ {

        @Test
        @DisplayName("returns mapped page when repository returns results")
        void returnsMappedPage() {
            given(repository.findAllFiltered(null, null, null, null, PageRequest.of(0, 20)))
                    .willReturn(new PageImpl<>(java.util.List.of(employee)));

            var page = service.list(null, null, null, null, PageRequest.of(0, 20));

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).email()).isEqualTo("alice@acme.com");
        }

        @Test
        @DisplayName("returns empty page when no employees match")
        void returnsEmptyPage() {
            given(repository.findAllFiltered(any(), any(), any(), any(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(java.util.List.of()));

            var page = service.list("Unknown Dept", null, null, null, PageRequest.of(0, 20));

            assertThat(page.getContent()).isEmpty();
        }

        @Test
        @DisplayName("passes filter params through to repository unchanged")
        void passesFiltersToRepository() {
            given(repository.findAllFiltered(eq("Engineering"), eq("US"), eq(EmployeeStatus.ACTIVE), eq("alice"), any(Pageable.class)))
                    .willReturn(new PageImpl<>(java.util.List.of(employee)));

            service.list("Engineering", "US", EmployeeStatus.ACTIVE, "alice", PageRequest.of(0, 10));

            then(repository).should().findAllFiltered("Engineering", "US", EmployeeStatus.ACTIVE, "alice", PageRequest.of(0, 10));
        }
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("returns response when employee exists")
        void returnsEmployeeWhenFound() {
            given(repository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));

            EmployeeResponse response = service.getById(EMPLOYEE_ID);

            assertThat(response.id()).isEqualTo(EMPLOYEE_ID);
            assertThat(response.fullName()).isEqualTo("Alice Smith");
            assertThat(response.status()).isEqualTo(EmployeeStatus.ACTIVE);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee does not exist")
        void throwsWhenNotFound() {
            given(repository.findById(UNKNOWN_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(UNKNOWN_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(UNKNOWN_ID.toString());
        }
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("saves employee and returns response with lowercased email")
        void savesAndReturnsResponse() {
            EmployeeRequest request = new EmployeeRequest(
                    "Alice Smith", "Alice@ACME.COM", "Engineering", "Software Engineer", "US");
            // Service calls existsByEmail with the raw email from the request (before lowercasing)
            given(repository.existsByEmail("Alice@ACME.COM")).willReturn(false);
            given(repository.save(any(Employee.class))).willReturn(employee);

            EmployeeResponse response = service.create(request);

            assertThat(response.email()).isEqualTo("alice@acme.com");
        }

        @Test
        @DisplayName("persists correct field values")
        void persistsCorrectFields() {
            given(repository.existsByEmail(anyString())).willReturn(false);
            given(repository.save(any(Employee.class))).willAnswer(inv -> inv.getArgument(0));

            service.create(validEmployeeRequest());

            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            then(repository).should().save(captor.capture());

            Employee saved = captor.getValue();
            assertThat(saved.getFullName()).isEqualTo("Alice Smith");
            assertThat(saved.getEmail()).isEqualTo("alice@acme.com");
            assertThat(saved.getDepartment()).isEqualTo("Engineering");
            assertThat(saved.getJobTitle()).isEqualTo("Software Engineer");
            assertThat(saved.getCountry()).isEqualTo("US");
            assertThat(saved.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        }

        @Test
        @DisplayName("throws ConflictException when email already exists")
        void throwsOnDuplicateEmail() {
            given(repository.existsByEmail("alice@acme.com")).willReturn(true);

            assertThatThrownBy(() -> service.create(validEmployeeRequest()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("alice@acme.com");
        }

        @Test
        @DisplayName("does not save when email already exists")
        void doesNotSaveOnDuplicateEmail() {
            given(repository.existsByEmail(anyString())).willReturn(true);

            assertThatThrownBy(() -> service.create(validEmployeeRequest()))
                    .isInstanceOf(ConflictException.class);

            then(repository).should(never()).save(any());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("updates and returns response when employee exists and email is unique")
        void updatesSuccessfully() {
            EmployeeRequest request = new EmployeeRequest(
                    "Alice Updated", "alice@acme.com", "Product", "PM", "GB");
            given(repository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
            given(repository.existsByEmailAndIdNot("alice@acme.com", EMPLOYEE_ID)).willReturn(false);
            given(repository.save(any())).willReturn(employee);

            EmployeeResponse response = service.update(EMPLOYEE_ID, request);

            assertThat(response).isNotNull();
            then(repository).should().save(employee);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee does not exist")
        void throwsWhenNotFound() {
            given(repository.findById(UNKNOWN_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(UNKNOWN_ID, validEmployeeRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws ConflictException when new email belongs to another employee")
        void throwsOnEmailTakenByOther() {
            given(repository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
            given(repository.existsByEmailAndIdNot("alice@acme.com", EMPLOYEE_ID)).willReturn(true);

            assertThatThrownBy(() -> service.update(EMPLOYEE_ID, validEmployeeRequest()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("alice@acme.com");
        }
    }

    // ── deactivate ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        @Test
        @DisplayName("sets status to INACTIVE and saves")
        void setsInactiveAndSaves() {
            given(repository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
            given(repository.save(employee)).willReturn(employee);

            EmployeeResponse response = service.deactivate(EMPLOYEE_ID);

            assertThat(response.status()).isEqualTo(EmployeeStatus.INACTIVE);
            then(repository).should().save(employee);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee does not exist")
        void throwsWhenNotFound() {
            given(repository.findById(UNKNOWN_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivate(UNKNOWN_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("idempotent — deactivating an already inactive employee still saves")
        void idempotentDeactivation() {
            Employee inactive = inactiveEmployee();
            given(repository.findById(EMPLOYEE_ID)).willReturn(Optional.of(inactive));
            given(repository.save(inactive)).willReturn(inactive);

            EmployeeResponse response = service.deactivate(EMPLOYEE_ID);

            assertThat(response.status()).isEqualTo(EmployeeStatus.INACTIVE);
        }
    }
}
