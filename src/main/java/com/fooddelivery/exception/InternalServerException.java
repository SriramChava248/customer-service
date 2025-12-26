package com.fooddelivery.exception;

/**
 * Exception thrown for internal server errors (unknown/unexpected errors).
 * Maps to HTTP 500 Internal Server Error.
 */
public class InternalServerException extends RuntimeException {
    
    public InternalServerException(String message) {
        super(message);
    }
    
    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }
}

