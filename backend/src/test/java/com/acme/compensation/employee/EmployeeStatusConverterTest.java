package com.acme.compensation.employee;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmployeeStatusConverter Unit Tests")
class EmployeeStatusConverterTest {

    private final EmployeeStatusConverter converter = new EmployeeStatusConverter();

    @ParameterizedTest
    @EnumSource(EmployeeStatus.class)
    @DisplayName("converts EmployeeStatus enum to lowercase database string")
    void convertToDatabaseColumn(EmployeeStatus status) {
        String dbValue = converter.convertToDatabaseColumn(status);
        assertThat(dbValue).isEqualTo(status.name().toLowerCase());
    }

    @Test
    @DisplayName("convertToDatabaseColumn returns null when status is null")
    void convertToDatabaseColumnNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    @DisplayName("converts lowercase database string to uppercase EmployeeStatus enum")
    void convertToEntityAttribute() {
        assertThat(converter.convertToEntityAttribute("active")).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(converter.convertToEntityAttribute("inactive")).isEqualTo(EmployeeStatus.INACTIVE);
        assertThat(converter.convertToEntityAttribute("ACTIVE")).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    @DisplayName("convertToEntityAttribute returns null when input is null or blank")
    void convertToEntityAttributeNullOrBlank() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("")).isNull();
        assertThat(converter.convertToEntityAttribute("   ")).isNull();
    }
}
