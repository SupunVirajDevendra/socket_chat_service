package com.supundevendra.chat.exception;

// Thrown when an ISO 8583 message cannot be unpacked, packed, or processed
public class IsoProcessingException extends ChatApplicationException {

    // Constructs the exception with a descriptive message
    public IsoProcessingException(String message) {
        super(message);
    }

    // Constructs the exception with a message and the underlying cause
    public IsoProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
