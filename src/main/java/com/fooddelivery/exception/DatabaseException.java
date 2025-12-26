package com.fooddelivery.exception;

/**
 * Exception thrown when database operations fail.
 * Maps to HTTP 502 Bad Gateway (service unavailable due to DB failure).
 */
public class DatabaseException extends RuntimeException {
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

