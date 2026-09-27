package com.acme.compensation.employee;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps the Java {@link EmployeeStatus} enum (ACTIVE, INACTIVE)
 * to the lowercase database check constraint values ('active', 'inactive').
 */
@Converter(autoApply = true)
public class EmployeeStatusConverter implements AttributeConverter<EmployeeStatus, String> {

    @Override
    public String convertToDatabaseColumn(EmployeeStatus status) {
        if (status == null) {
            return null;
        }
        return status.name().toLowerCase();
    }

    @Override
    public EmployeeStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return EmployeeStatus.valueOf(dbData.trim().toUpperCase());
    }
}
