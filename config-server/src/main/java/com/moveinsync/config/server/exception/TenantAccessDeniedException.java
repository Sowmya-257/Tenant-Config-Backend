package com.moveinsync.config.server.exception;

/**
 * Thrown when an authenticated user attempts to access another tenant's configuration data.
 */
public class TenantAccessDeniedException extends RuntimeException {

    // Constructs a tenant isolation access denied exception.
    public TenantAccessDeniedException(String message) {
        super(message);
    }
}
