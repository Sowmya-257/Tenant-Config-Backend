package com.moveinsync.config.server.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler providing clean, standardized, non-leaking error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handles setting validation errors before changes are committed or published.
    @ExceptionHandler(InvalidConfigException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidConfig(InvalidConfigException ex) {
        log.warn("Invalid configuration rejected: {}", ex.getMessage());
        Map<String, Object> body = buildErrorBody(HttpStatus.BAD_REQUEST, "INVALID_CONFIG_VALUE", ex.getMessage());
        body.put("settingKey", ex.getKey());
        body.put("rejectedValue", ex.getRejectedValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Handles missing resource errors.
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorBody(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    // Handles cross-tenant access violations enforcing strict multi-tenant isolation.
    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleTenantAccessDenied(TenantAccessDeniedException ex) {
        log.warn("Tenant isolation violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildErrorBody(HttpStatus.FORBIDDEN, "TENANT_ACCESS_DENIED", ex.getMessage()));
    }

    // Handles HTTP request bean validation errors.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = buildErrorBody(HttpStatus.BAD_REQUEST, "ARGUMENT_VALIDATION_FAILED", "Validation failed for request parameters");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Handles illegal arguments and invalid states.
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleIllegalArguments(RuntimeException ex) {
        log.warn("Illegal argument or state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorBody(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage()));
    }

    // Handles unexpected server exceptions, masking internal stack traces to prevent leakage.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled internal server error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected server error occurred. Please try again."));
    }

    // Helper that constructs standardized JSON error response map without leaking PII.
    private Map<String, Object> buildErrorBody(HttpStatus status, String errorCode, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("errorCode", errorCode);
        body.put("message", message);
        return body;
    }
}
