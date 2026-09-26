package com.acme.compensation.employee.dto;

import com.acme.compensation.employee.Employee;
import com.acme.compensation.employee.EmployeeStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * API response DTO for employee records.
 * Salary is deliberately absent — it is returned only by the compensation endpoints.
 */
public record EmployeeResponse(
        UUID id,
        String fullName,
        String email,
        String department,
        String jobTitle,
        String country,
        EmployeeStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(
                e.getId(),
                e.getFullName(),
                e.getEmail(),
                e.getDepartment(),
                e.getJobTitle(),
                e.getCountry(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
