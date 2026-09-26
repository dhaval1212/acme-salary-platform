package com.acme.compensation.common;

/**
 * Field-level error detail included in the errors array of ApiResponse.
 *
 * @param field   the request field that failed validation, or "global" for non-field errors
 * @param message human-readable description of the failure
 */
public record ApiError(String field, String message) {

    public static ApiError global(String message) {
        return new ApiError("global", message);
    }
}
