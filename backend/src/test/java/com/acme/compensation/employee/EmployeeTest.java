package com.acme.compensation.employee;

import com.acme.compensation.employee.dto.EmployeeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Employee Entity & DTO Unit Tests")
class EmployeeTest {

    @Test
    @DisplayName("defaults status to ACTIVE")
    void defaultStatusIsActive() {
        Employee employee = new Employee();
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    @DisplayName("onCreate sets createdAt and updatedAt on pre-persist")
    void onCreateSetsTimestamps() throws Exception {
        Employee employee = new Employee();
        assertThat(employee.getCreatedAt()).isNull();
        assertThat(employee.getUpdatedAt()).isNull();

        Method onCreate = Employee.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(employee);

        assertThat(employee.getCreatedAt()).isNotNull();
        assertThat(employee.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onUpdate updates updatedAt timestamp on pre-update")
    void onUpdateUpdatesTimestamp() throws Exception {
        Employee employee = new Employee();

        Method onCreate = Employee.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(employee);

        Instant initialUpdatedAt = employee.getUpdatedAt();

        Thread.sleep(10);

        Method onUpdate = Employee.class.getDeclaredMethod("onUpdate");
        onUpdate.setAccessible(true);
        onUpdate.invoke(employee);

        assertThat(employee.getUpdatedAt()).isAfterOrEqualTo(initialUpdatedAt);
    }

    @Test
    @DisplayName("EmployeeResponse.from properly maps all Employee properties")
    void mapsToEmployeeResponse() throws Exception {
        Employee employee = new Employee();
        employee.setFullName("John Doe");
        employee.setEmail("john.doe@acme.com");
        employee.setDepartment("Security");
        employee.setJobTitle("Security Engineer");
        employee.setCountry("Germany");
        employee.setStatus(EmployeeStatus.ACTIVE);

        Method onCreate = Employee.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(employee);

        EmployeeResponse response = EmployeeResponse.from(employee);

        assertThat(response.fullName()).isEqualTo("John Doe");
        assertThat(response.email()).isEqualTo("john.doe@acme.com");
        assertThat(response.department()).isEqualTo("Security");
        assertThat(response.jobTitle()).isEqualTo("Security Engineer");
        assertThat(response.country()).isEqualTo("Germany");
        assertThat(response.status()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(response.createdAt()).isEqualTo(employee.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(employee.getUpdatedAt());
    }
}
