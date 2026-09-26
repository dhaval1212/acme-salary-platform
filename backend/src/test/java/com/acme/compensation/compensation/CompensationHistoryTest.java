package com.acme.compensation.compensation;

import com.acme.compensation.compensation.dto.CompensationResponse;
import com.acme.compensation.employee.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CompensationHistory Entity & DTO Unit Tests")
class CompensationHistoryTest {

    @Test
    @DisplayName("onCreate sets createdAt timestamp on pre-persist")
    void onCreateSetsCreatedAt() throws Exception {
        CompensationHistory history = new CompensationHistory();
        assertThat(history.getCreatedAt()).isNull();

        Method onCreate = CompensationHistory.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(history);

        assertThat(history.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("CompensationResponse.from properly maps all CompensationHistory properties")
    void mapsToCompensationResponse() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID historyId = UUID.randomUUID();

        Employee employee = new Employee();
        Field idField = Employee.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(employee, employeeId);

        CompensationHistory history = new CompensationHistory();
        Field historyIdField = CompensationHistory.class.getDeclaredField("id");
        historyIdField.setAccessible(true);
        historyIdField.set(history, historyId);

        history.setEmployee(employee);
        history.setAmount(new BigDecimal("145000.00"));
        history.setCurrency("EUR");
        history.setEffectiveDate(LocalDate.of(2025, 3, 15));
        history.setChangedBy("manager@acme.com");

        Method onCreate = CompensationHistory.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(history);

        CompensationResponse response = CompensationResponse.from(history);

        assertThat(response.id()).isEqualTo(historyId);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.amount()).isEqualByComparingTo("145000.00");
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.effectiveDate()).isEqualTo(LocalDate.of(2025, 3, 15));
        assertThat(response.changedBy()).isEqualTo("manager@acme.com");
        assertThat(response.createdAt()).isEqualTo(history.getCreatedAt());
    }
}
