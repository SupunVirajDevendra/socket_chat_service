package com.supundevendra.chat.exception;

// Thrown when configuration is missing, invalid, or cannot be loaded
public class ConfigurationException extends ChatApplicationException {

    // Constructs the exception with a descriptive message
    public ConfigurationException(String message) {
        super(message);
    }

    // Constructs the exception with a message and the underlying cause
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
