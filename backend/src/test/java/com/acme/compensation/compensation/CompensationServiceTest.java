package com.acme.compensation.compensation;

import com.acme.compensation.TestFixtures;
import com.acme.compensation.common.ConflictException;
import com.acme.compensation.common.ResourceNotFoundException;
import com.acme.compensation.compensation.dto.CompensationRequest;
import com.acme.compensation.compensation.dto.CompensationResponse;
import com.acme.compensation.employee.Employee;
import com.acme.compensation.employee.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.acme.compensation.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for CompensationService.
 * Both repositories are mocked — no database or Spring context required.
 */
@ExtendWith(MockitoExtension.class)
class CompensationServiceTest {

    @Mock
    CompensationRepository compensationRepository;

    @Mock
    EmployeeRepository employeeRepository;

    @InjectMocks
    CompensationService service;

    Employee employee;
    CompensationHistory entry;

    @BeforeEach
    void setUp() {
        employee = activeEmployee();
        entry = compensationEntry(employee, new BigDecimal("90000.00"), LocalDate.of(2026, 1, 1));
    }

    // ── getCurrent ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCurrent()")
    class GetCurrent {

        @Test
        @DisplayName("returns current compensation when employee and record exist")
        void returnsCurrentCompensation() {
            given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
            given(compensationRepository.findCurrentByEmployeeId(eq(EMPLOYEE_ID), any(LocalDate.class)))
                    .willReturn(Optional.of(entry));

            CompensationResponse response = service.getCurrent(EMPLOYEE_ID);

            assertThat(response.amount()).isEqualByComparingTo("90000.00");
            assertThat(response.currency()).isEqualTo("USD");
            assertThat(response.effectiveDate()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(response.employeeId()).isEqualTo(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee does not exist")
        void throwsWhenEmployeeNotFound() {
            given(employeeRepository.existsById(UNKNOWN_ID)).willReturn(false);

            assertThatThrownBy(() -> service.getCurrent(UNKNOWN_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(UNKNOWN_ID.toString());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when no compensation record exists yet")
        void throwsWhenNoCompensationRecord() {
            given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
            given(compensationRepository.findCurrentByEmployeeId(eq(EMPLOYEE_ID), any(LocalDate.class)))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCurrent(EMPLOYEE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("uses today's date to resolve current salary (boundary)")
        void usesTodayAsAsOfDate() {
            given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
            given(compensationRepository.findCurrentByEmployeeId(eq(EMPLOYEE_ID), any(LocalDate.class)))
                    .willReturn(Optional.of(entry));

            service.getCurrent(EMPLOYEE_ID);

            // Verify asOf is passed (today), not a fixed past/future date
            ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
            then(compensationRepository).should()
                    .findCurrentByEmployeeId(eq(EMPLOYEE_ID), captor.capture());
            assertThat(captor.getValue()).isEqualTo(LocalDate.now());
        }
    }

    // ── getHistory ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHistory()")
    class GetHistory {

        @Test
        @DisplayName("returns all history entries newest first")
        void returnsFullHistory() {
            CompensationHistory older = compensationEntry(employee,
                    new BigDecimal("80000.00"), LocalDate.of(2025, 1, 1));
            given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
            given(compensationRepository.findHistoryByEmployeeId(EMPLOYEE_ID))
                    .willReturn(List.of(entry, older)); // service returns what repo gives

            List<CompensationResponse> history = service.getHistory(EMPLOYEE_ID);

            assertThat(history).hasSize(2);
            assertThat(history.get(0).amount()).isEqualByComparingTo("90000.00");
            assertThat(history.get(1).amount()).isEqualByComparingTo("80000.00");
        }

        @Test
        @DisplayName("returns empty list when employee has no compensation records")
        void returnsEmptyListWhenNoRecords() {
            given(employeeRepository.existsById(EMPLOYEE_ID)).willReturn(true);
            given(compensationRepository.findHistoryByEmployeeId(EMPLOYEE_ID)).willReturn(List.of());

            List<CompensationResponse> history = service.getHistory(EMPLOYEE_ID);

            assertThat(history).isEmpty();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee does not exist")
        void throwsWhenEmployeeNotFound() {
            given(employeeRepository.existsById(UNKNOWN_ID)).willReturn(false);

            assertThatThrownBy(() -> service.getHistory(UNKNOWN_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── record ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("record()")
    class Record {

        @Test
        @DisplayName("saves new compensation entry and returns response")
        void savesAndReturnsResponse() {
            given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
            given(compensationRepository.existsByEmployeeIdAndEffectiveDate(
                    EMPLOYEE_ID, LocalDate.of(2026, 1, 1))).willReturn(false);
            given(compensationRepository.save(any())).willReturn(entry);

            CompensationResponse response = service.record(EMPLOYEE_ID, validCompensationRequest());

            assertThat(response.amount()).isEqualByComparingTo("90000.00");
            assertThat(response.currency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("normalises currency to uppercase before saving")
        void normalisesCurrencyToUppercase() {
            CompensationRequest request = new CompensationRequest(
                    new BigDecimal("90000.00"), "usd", LocalDate.of(2026, 1, 1), "hr@acme.com");
            given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
            given(compensationRepository.existsByEmployeeIdAndEffectiveDate(any(), any())).willReturn(false);
            given(compensationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.record(EMPLOYEE_ID, request);

            ArgumentCaptor<CompensationHistory> captor = ArgumentCaptor.forClass(CompensationHistory.class);
            then(compensationRepository).should().save(captor.capture());
            assertThat(captor.getValue().getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee does not exist")
        void throwsWhenEmployeeNotFound() {
            given(employeeRepository.findById(UNKNOWN_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.record(UNKNOWN_ID, validCompensationRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(UNKNOWN_ID.toString());
        }

        @Test
        @DisplayName("throws ConflictException when a record already exists for this employee on this date")
        void throwsOnDuplicateEffectiveDate() {
            given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
            given(compensationRepository.existsByEmployeeIdAndEffectiveDate(
                    EMPLOYEE_ID, LocalDate.of(2026, 1, 1))).willReturn(true);

            assertThatThrownBy(() -> service.record(EMPLOYEE_ID, validCompensationRequest()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("2026-01-01");
        }

        @Test
        @DisplayName("does not save when duplicate effective date detected")
        void doesNotSaveOnDuplicate() {
            given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
            given(compensationRepository.existsByEmployeeIdAndEffectiveDate(any(), any())).willReturn(true);

            assertThatThrownBy(() -> service.record(EMPLOYEE_ID, validCompensationRequest()))
                    .isInstanceOf(ConflictException.class);

            then(compensationRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("amount is persisted exactly as provided — no rounding by service layer")
        void persistsExactAmount() {
            BigDecimal exactAmount = new BigDecimal("123456.78");
            CompensationRequest request = new CompensationRequest(
                    exactAmount, "GBP", LocalDate.of(2026, 3, 1), "hr@acme.com");
            given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
            given(compensationRepository.existsByEmployeeIdAndEffectiveDate(any(), any())).willReturn(false);
            given(compensationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.record(EMPLOYEE_ID, request);

            ArgumentCaptor<CompensationHistory> captor = ArgumentCaptor.forClass(CompensationHistory.class);
            then(compensationRepository).should().save(captor.capture());
            assertThat(captor.getValue().getAmount()).isEqualByComparingTo(exactAmount);
        }
    }
}
