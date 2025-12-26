package com.fooddelivery.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard error response structure for all API errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * HTTP status code (400, 404, 500, 502, etc.)
     */
    private int status;
    
    /**
     * Error type/category (e.g., "CustomerNotFoundException", "BadRequestException")
     */
    private String error;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * Timestamp when error occurred
     */
    private Instant timestamp;
    
    /**
     * Request path that caused the error
     */
    private String path;
    
    /**
     * Stack trace (only included in development/debug mode)
     */
    private String stackTrace;
}

