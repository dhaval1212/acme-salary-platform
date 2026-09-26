package com.acme.compensation.compensation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompensationRepository extends JpaRepository<CompensationHistory, UUID> {

    /**
     * Current salary: most recent record with effective_date <= today.
     */
    @Query("""
            SELECT c FROM CompensationHistory c
            WHERE c.employee.id = :employeeId
              AND c.effectiveDate <= :asOf
            ORDER BY c.effectiveDate DESC
            LIMIT 1
            """)
    Optional<CompensationHistory> findCurrentByEmployeeId(
            @Param("employeeId") UUID employeeId,
            @Param("asOf") LocalDate asOf
    );

    /**
     * Full compensation history for an employee, newest first.
     */
    @Query("""
            SELECT c FROM CompensationHistory c
            WHERE c.employee.id = :employeeId
            ORDER BY c.effectiveDate DESC
            """)
    List<CompensationHistory> findHistoryByEmployeeId(@Param("employeeId") UUID employeeId);

    boolean existsByEmployeeIdAndEffectiveDate(UUID employeeId, LocalDate effectiveDate);
}
