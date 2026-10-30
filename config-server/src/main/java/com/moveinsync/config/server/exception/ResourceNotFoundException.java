package com.moveinsync.config.server.exception;

/**
 * Thrown when a requested resource like a definition or rollout is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    // Constructs a resource not found exception with a message.
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
