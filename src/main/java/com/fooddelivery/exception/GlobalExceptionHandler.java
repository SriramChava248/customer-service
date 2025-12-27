package com.fooddelivery.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

/**
 * Global exception handler that catches all exceptions thrown throughout the application.
 * 
 * Responsibilities:
 * 1. Catch all exceptions (known and unknown)
 * 2. Map exceptions to appropriate HTTP status codes:
 *    - 400 Bad Request: BadRequestException
 *    - 401 Unauthorized: UnauthorizedException
 *    - 404 Not Found: CustomerNotFoundException
 *    - 500 Internal Server Error: InternalServerException, unknown exceptions
 *    - 502 Bad Gateway: DatabaseException
 * 3. Provide structured error responses with stack traces for debugging
 * 4. Log all exceptions for investigation
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Whether to include stack traces in error responses.
     * Set to true in development, false in production.
     */
    @Value("${app.debug.stacktrace:true}")
    private boolean includeStackTrace;

    /**
     * Handle CustomerNotFoundException - 404 Not Found
     */
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFoundException(
            CustomerNotFoundException ex,
            HttpServletRequest request) {
        
        log.warn("Customer not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "CustomerNotFoundException",
                ex.getMessage(),
                request.getRequestURI(),
                includeStackTrace ? getStackTrace(ex) : null
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle BadRequestException - 400 Bad Request
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex,
            HttpServletRequest request) {
        
        log.warn("Bad request: {}", ex.getMessage());
        
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "BadRequestException",
                ex.getMessage(),
                request.getRequestURI(),
                includeStackTrace ? getStackTrace(ex) : null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle UnauthorizedException - 401 Unauthorized
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex,
            HttpServletRequest request) {
        
        log.warn("Unauthorized access: {}", ex.getMessage());
        
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "UnauthorizedException",
                ex.getMessage(),
                request.getRequestURI(),
                includeStackTrace ? getStackTrace(ex) : null
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle DatabaseException - 502 Bad Gateway
     * Database failures indicate the service is unavailable due to external dependency failure
     */
    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseException(
            DatabaseException ex,
            HttpServletRequest request) {
        
        log.error("Database error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_GATEWAY.value(),
                "DatabaseException",
                ex.getMessage(),
                request.getRequestURI(),
                includeStackTrace ? getStackTrace(ex) : null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    /**
     * Handle InternalServerException - 500 Internal Server Error
     */
    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ErrorResponse> handleInternalServerException(
            InternalServerException ex,
            HttpServletRequest request) {
        
        log.error("Internal server error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "InternalServerException",
                ex.getMessage(),
                request.getRequestURI(),
                includeStackTrace ? getStackTrace(ex) : null
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Catch-all handler for any unknown/unexpected exceptions.
     * This ensures no exception goes unhandled.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknownException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Unknown exception occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getClass().getSimpleName(),
                "An unexpected error occurred: " + ex.getMessage(),
                request.getRequestURI(),
                includeStackTrace ? getStackTrace(ex) : null
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Build ErrorResponse object with all details.
     */
    private ErrorResponse buildErrorResponse(
            int status,
            String error,
            String message,
            String path,
            String stackTrace) {
        
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .timestamp(Instant.now())
                .path(path)
                .stackTrace(stackTrace)
                .build();
    }

    /**
     * Convert exception stack trace to string for error response.
     */
    private String getStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }
}

