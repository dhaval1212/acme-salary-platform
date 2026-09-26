package com.acme.compensation;

import com.acme.compensation.compensation.CompensationHistory;
import com.acme.compensation.compensation.dto.CompensationRequest;
import com.acme.compensation.compensation.dto.CompensationResponse;
import com.acme.compensation.employee.Employee;
import com.acme.compensation.employee.EmployeeStatus;
import com.acme.compensation.employee.dto.EmployeeRequest;
import com.acme.compensation.employee.dto.EmployeeResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Shared, deterministic test fixtures.
 * All IDs and values are fixed so assertions are stable across runs.
 */
public final class TestFixtures {

    private TestFixtures() {}

    // ── Fixed IDs ────────────────────────────────────────────────────────────
    public static final UUID EMPLOYEE_ID   = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    public static final UUID EMPLOYEE_ID_2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    public static final UUID COMP_ID       = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    public static final UUID UNKNOWN_ID    = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    // ── Employee builders ────────────────────────────────────────────────────

    public static Employee activeEmployee() {
        Employee e = new Employee();
        setId(e, EMPLOYEE_ID);
        e.setFullName("Alice Smith");
        e.setEmail("alice@acme.com");
        e.setDepartment("Engineering");
        e.setJobTitle("Software Engineer");
        e.setCountry("US");
        e.setStatus(EmployeeStatus.ACTIVE);
        setTimestamps(e);
        return e;
    }

    public static Employee inactiveEmployee() {
        Employee e = activeEmployee();
        e.setStatus(EmployeeStatus.INACTIVE);
        return e;
    }

    public static EmployeeRequest validEmployeeRequest() {
        return new EmployeeRequest(
                "Alice Smith",
                "alice@acme.com",
                "Engineering",
                "Software Engineer",
                "US"
        );
    }

    public static EmployeeResponse employeeResponse(Employee e) {
        return EmployeeResponse.from(e);
    }

    // ── Compensation builders ────────────────────────────────────────────────

    public static CompensationHistory compensationEntry(Employee employee, BigDecimal amount, LocalDate effectiveDate) {
        CompensationHistory c = new CompensationHistory();
        setId(c, COMP_ID);
        c.setEmployee(employee);
        c.setAmount(amount);
        c.setCurrency("USD");
        c.setEffectiveDate(effectiveDate);
        c.setChangedBy("hr-manager@acme.com");
        setCreatedAt(c);
        return c;
    }

    public static CompensationRequest validCompensationRequest() {
        return new CompensationRequest(
                new BigDecimal("90000.00"),
                "USD",
                LocalDate.of(2026, 1, 1),
                "hr-manager@acme.com"
        );
    }

    public static CompensationResponse compensationResponse(CompensationHistory c) {
        return CompensationResponse.from(c);
    }

    // ── Reflection helpers (set private/no-setter fields on entities) ────────

    public static void setId(Employee employee, UUID id) {
        try {
            var field = Employee.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(employee, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void setId(CompensationHistory ch, UUID id) {
        try {
            var field = CompensationHistory.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(ch, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void setTimestamps(Employee e) {
        try {
            var created = Employee.class.getDeclaredField("createdAt");
            created.setAccessible(true);
            created.set(e, Instant.parse("2025-01-01T00:00:00Z"));

            var updated = Employee.class.getDeclaredField("updatedAt");
            updated.setAccessible(true);
            updated.set(e, Instant.parse("2025-01-01T00:00:00Z"));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void setCreatedAt(CompensationHistory c) {
        try {
            var field = CompensationHistory.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(c, Instant.parse("2025-01-01T00:00:00Z"));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
