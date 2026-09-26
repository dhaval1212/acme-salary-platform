package com.acme.compensation.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleValidation converts MethodArgumentNotValidException into 400 with field errors")
    void handlesValidationException() throws NoSuchMethodException {
        TestTarget target = new TestTarget();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "testTarget");
        bindingResult.addError(new FieldError("testTarget", "email", "Must be a valid email address"));
        bindingResult.addError(new FieldError("testTarget", "fullName", "Full name is required"));

        Method method = getClass().getDeclaredMethod("dummyMethod", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();

        List<ApiError> errors = response.getBody().errors();
        assertThat(errors).hasSize(2);
        assertThat(errors).extracting(ApiError::field).containsExactlyInAnyOrder("email", "fullName");
        assertThat(errors).extracting(ApiError::message).containsExactlyInAnyOrder("Must be a valid email address", "Full name is required");
    }

    @Test
    @DisplayName("handleNotFound converts ResourceNotFoundException into 404 with global error")
    void handlesResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Employee", "123e4567-e89b-12d3-a456-426614174000");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().errors()).hasSize(1);
        assertThat(response.getBody().errors().get(0).field()).isEqualTo("global");
        assertThat(response.getBody().errors().get(0).message()).isEqualTo("Employee not found: 123e4567-e89b-12d3-a456-426614174000");
    }

    @Test
    @DisplayName("handleConflict converts ConflictException into 409 with global error")
    void handlesConflictException() {
        ConflictException ex = new ConflictException("Email already exists");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleConflict(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().errors()).hasSize(1);
        assertThat(response.getBody().errors().get(0).field()).isEqualTo("global");
        assertThat(response.getBody().errors().get(0).message()).isEqualTo("Email already exists");
    }

    @Test
    @DisplayName("handleUnexpected converts unhandled Exception into 500 without leaking sensitive internal details")
    void handlesUnexpectedExceptionSafely() {
        Exception ex = new RuntimeException("Sensitive database constraint with salary $500,000 details");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleUnexpected(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().errors()).hasSize(1);
        assertThat(response.getBody().errors().get(0).field()).isEqualTo("global");
        assertThat(response.getBody().errors().get(0).message()).isEqualTo("An unexpected error occurred");
    }

    @SuppressWarnings("unused")
    private void dummyMethod(String param) {}

    private static class TestTarget {}
}
