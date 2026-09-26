package com.acme.compensation.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Standard JSON envelope for all API responses.
 * <pre>
 * {
 *   "data":   { ... } or [ ... ],
 *   "meta":   { "page": 0, "pageSize": 20, ... },
 *   "errors": [ { "field": "email", "message": "must not be blank" } ]
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        T data,
        Object meta,
        List<ApiError> errors
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, Object meta) {
        return new ApiResponse<>(data, meta, null);
    }

    public static <T> ApiResponse<T> error(List<ApiError> errors) {
        return new ApiResponse<>(null, null, errors);
    }

    public static <T> ApiResponse<T> error(String field, String message) {
        return error(List.of(new ApiError(field, message)));
    }
}
