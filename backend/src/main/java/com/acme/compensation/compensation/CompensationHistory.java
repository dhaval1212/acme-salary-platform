package com.acme.compensation.compensation;

import com.acme.compensation.employee.Employee;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Append-only record of every salary change for an employee.
 * Rows are never updated or deleted — current salary is derived by querying
 * the most recent row with effective_date <= today.
 */
@Entity
@Table(name = "compensation_history")
public class CompensationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, updatable = false)
    private Employee employee;

    /**
     * Stored as NUMERIC(15,2). Never exposed in logs.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * ISO 4217 three-letter currency code, stored uppercase.
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * The date from which this compensation applies. Set by the HR Manager.
     */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    /**
     * Identity of the HR Manager who recorded the change.
     */
    @Column(name = "changed_by", nullable = false, length = 255)
    private String changedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // ── Getters & setters ────────────────────────────────────────────────────

    public UUID getId() { return id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public Instant getCreatedAt() { return createdAt; }
}
