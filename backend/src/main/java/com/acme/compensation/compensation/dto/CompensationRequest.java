package com.acme.compensation.compensation.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompensationRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Amount must be zero or positive")
        @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 integer digits and 2 decimal places")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 three-letter code (e.g. USD)")
        String currency,

        @NotNull(message = "Effective date is required")
        LocalDate effectiveDate,

        @NotBlank(message = "Changed by is required")
        @Size(max = 255)
        String changedBy
) {
}
