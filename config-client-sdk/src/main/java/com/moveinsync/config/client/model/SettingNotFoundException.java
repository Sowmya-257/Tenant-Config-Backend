package com.moveinsync.config.client.model;

/**
 * Thrown when a requested configuration key is not defined at any level.
 */
public class SettingNotFoundException extends RuntimeException {

    // Constructs a new exception with a descriptive message.
    public SettingNotFoundException(String message) {
        super(message);
    }
}
