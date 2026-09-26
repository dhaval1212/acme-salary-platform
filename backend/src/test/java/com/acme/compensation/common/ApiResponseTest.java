package com.acme.compensation.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse & Common Envelope Unit Tests")
class ApiResponseTest {

    @Test
    @DisplayName("ApiResponse.ok(data) wraps data with null meta and errors")
    void okWithDataOnly() {
        ApiResponse<String> response = ApiResponse.ok("success-payload");

        assertThat(response.data()).isEqualTo("success-payload");
        assertThat(response.meta()).isNull();
        assertThat(response.errors()).isNull();
    }

    @Test
    @DisplayName("ApiResponse.ok(data, meta) wraps data and pagination meta")
    void okWithDataAndMeta() {
        PageMeta meta = new PageMeta(1, 20, 100L, 5);
        ApiResponse<List<String>> response = ApiResponse.ok(List.of("item1", "item2"), meta);

        assertThat(response.data()).containsExactly("item1", "item2");
        assertThat(response.meta()).isEqualTo(meta);
        assertThat(response.errors()).isNull();
    }

    @Test
    @DisplayName("ApiResponse.error(errors) wraps error list with null data and meta")
    void errorWithList() {
        List<ApiError> errors = List.of(new ApiError("email", "invalid"), new ApiError("name", "required"));
        ApiResponse<Void> response = ApiResponse.error(errors);

        assertThat(response.data()).isNull();
        assertThat(response.meta()).isNull();
        assertThat(response.errors()).isEqualTo(errors);
    }

    @Test
    @DisplayName("ApiResponse.error(field, message) wraps single field error")
    void errorWithFieldAndMessage() {
        ApiResponse<Void> response = ApiResponse.error("country", "Country is required");

        assertThat(response.data()).isNull();
        assertThat(response.meta()).isNull();
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().get(0).field()).isEqualTo("country");
        assertThat(response.errors().get(0).message()).isEqualTo("Country is required");
    }

    @Test
    @DisplayName("ApiError.global sets field to 'global'")
    void apiErrorGlobal() {
        ApiError error = ApiError.global("System down");

        assertThat(error.field()).isEqualTo("global");
        assertThat(error.message()).isEqualTo("System down");
    }

    @Test
    @DisplayName("PageMeta stores page, pageSize, totalElements, and totalPages")
    void pageMetaProperties() {
        PageMeta meta = new PageMeta(2, 25, 120L, 5);

        assertThat(meta.page()).isEqualTo(2);
        assertThat(meta.pageSize()).isEqualTo(25);
        assertThat(meta.totalElements()).isEqualTo(120L);
        assertThat(meta.totalPages()).isEqualTo(5);
    }
}
