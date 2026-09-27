package com.acme.compensation.analytics.dto;

import java.math.BigDecimal;

/**
 * Salary statistics for a single department and country/currency.
 * Projected directly from the native SQL analytics query.
 */
public record DepartmentStats(
        String department,
        String country,
        String currency,
        long headcount,
        BigDecimal avgSalary,
        BigDecimal medianSalary,
        BigDecimal minSalary,
        BigDecimal maxSalary
) {
    public DepartmentStats(
            String department,
            long headcount,
            BigDecimal avgSalary,
            BigDecimal medianSalary,
            BigDecimal minSalary,
            BigDecimal maxSalary
    ) {
        this(department, null, "USD", headcount, avgSalary, medianSalary, minSalary, maxSalary);
    }
}

