package com.acme.compensation.analytics;

import com.acme.compensation.analytics.dto.DepartmentStats;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Read-only analytics repository.
 * All queries use native SQL — JPQL cannot express PERCENTILE_CONT.
 */
public interface AnalyticsRepository extends Repository<Object, UUID> {

    /**
     * Per-department salary statistics for active employees.
     * Filters by department and country if provided (null = no filter).
     */
    @Query(value = """
            SELECT
                e.department,
                COUNT(*)                                                            AS headcount,
                ROUND(AVG(c.amount)::NUMERIC, 2)                                   AS avg_salary,
                ROUND(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY c.amount)::NUMERIC, 2)
                                                                                   AS median_salary,
                MIN(c.amount)                                                       AS min_salary,
                MAX(c.amount)                                                       AS max_salary
            FROM employees e
            JOIN (
                SELECT DISTINCT ON (employee_id)
                    employee_id, amount
                FROM compensation_history
                WHERE effective_date <= :asOf
                ORDER BY employee_id, effective_date DESC
            ) c ON c.employee_id = e.id
            WHERE e.status = 'active'
              AND (:department IS NULL OR e.department = :department)
              AND (:country    IS NULL OR e.country    = :country)
            GROUP BY e.department
            ORDER BY e.department
            """, nativeQuery = true)
    List<Object[]> findDepartmentStatsRaw(
            @Param("asOf") LocalDate asOf,
            @Param("department") String department,
            @Param("country") String country
    );
}
