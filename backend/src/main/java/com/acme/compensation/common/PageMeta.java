package com.acme.compensation.common;

/**
 * Pagination metadata returned in the meta field of paginated list responses.
 */
public record PageMeta(
        int page,
        int pageSize,
        long totalElements,
        int totalPages
) {
}
