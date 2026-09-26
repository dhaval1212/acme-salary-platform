package com.acme.compensation.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    Optional<Employee> findByEmail(String email);

    /**
     * Filtered, paginated employee list.
     * All filter params are optional — null values are ignored.
     */
    @Query("""
            SELECT e FROM Employee e
            WHERE (:department IS NULL OR e.department = :department)
              AND (:country    IS NULL OR e.country    = :country)
              AND (:status     IS NULL OR e.status     = :status)
              AND (:search     IS NULL
                   OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(e.email)    LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Employee> findAllFiltered(
            @Param("department") String department,
            @Param("country") String country,
            @Param("status") EmployeeStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
