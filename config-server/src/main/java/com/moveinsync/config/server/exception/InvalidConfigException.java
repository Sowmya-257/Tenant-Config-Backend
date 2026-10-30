package com.moveinsync.config.server.exception;

/**
 * Thrown when an invalid configuration value is rejected prior to publication.
 */
public class InvalidConfigException extends RuntimeException {

    private final String key;
    private final String rejectedValue;

    // Constructs an exception detailing the rejected setting key and reason.
    public InvalidConfigException(String key, String rejectedValue, String reason) {
        super("Validation failed for setting '" + key + "' with value '" + rejectedValue + "': " + reason);
        this.key = key;
        this.rejectedValue = rejectedValue;
    }

    // Returns the setting key that failed validation.
    public String getKey() {
        return key;
    }

    // Returns the invalid value that was rejected.
    public String getRejectedValue() {
        return rejectedValue;
    }
}
