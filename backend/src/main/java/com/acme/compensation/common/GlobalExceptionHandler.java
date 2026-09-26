package com.acme.compensation.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Centralised exception handling.  All salary-related fields are excluded
 * from log output — enforce this here and in service-layer logging.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(ApiResponse.error(errors));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(List.of(ApiError.global(ex.getMessage()))));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(List.of(ApiError.global(ex.getMessage()))));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        // Log without any compensation/salary data in the message
        log.error("Unexpected error: {}", ex.getClass().getSimpleName());
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error(List.of(ApiError.global("An unexpected error occurred"))));
    }
}
