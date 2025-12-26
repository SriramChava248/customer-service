package com.fooddelivery.exception;

/**
 * Exception thrown for bad request (validation errors, invalid input).
 * Maps to HTTP 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {
    
    public BadRequestException(String message) {
        super(message);
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

