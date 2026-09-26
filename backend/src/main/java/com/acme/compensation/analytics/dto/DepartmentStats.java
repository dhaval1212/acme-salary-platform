package com.acme.compensation.analytics.dto;

import java.math.BigDecimal;

/**
 * Salary statistics for a single department.
 * Projected directly from the native SQL analytics query.
 */
public record DepartmentStats(
        String department,
        long headcount,
        BigDecimal avgSalary,
        BigDecimal medianSalary,
        BigDecimal minSalary,
        BigDecimal maxSalary
) {
}
