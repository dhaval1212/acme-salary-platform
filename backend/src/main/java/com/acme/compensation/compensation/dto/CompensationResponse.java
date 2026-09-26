package com.acme.compensation.compensation.dto;

import com.acme.compensation.compensation.CompensationHistory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CompensationResponse(
        UUID id,
        UUID employeeId,
        BigDecimal amount,
        String currency,
        LocalDate effectiveDate,
        String changedBy,
        Instant createdAt
) {

    public static CompensationResponse from(CompensationHistory c) {
        return new CompensationResponse(
                c.getId(),
                c.getEmployee().getId(),
                c.getAmount(),
                c.getCurrency(),
                c.getEffectiveDate(),
                c.getChangedBy(),
                c.getCreatedAt()
        );
    }
}
