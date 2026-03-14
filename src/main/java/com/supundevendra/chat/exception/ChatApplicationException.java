package com.supundevendra.chat.exception;

// Abstract base for all application-specific exceptions in this project.
// Catching this type covers any error thrown by application code without
// catching unrelated Java library exceptions.
public abstract class ChatApplicationException extends RuntimeException {

    // Constructs the exception with a descriptive message
    public ChatApplicationException(String message) {
        super(message);
    }

    // Constructs the exception with a message and the underlying cause
    public ChatApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
