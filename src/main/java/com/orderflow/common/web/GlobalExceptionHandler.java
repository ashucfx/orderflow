package com.orderflow.common.web;

import com.orderflow.common.api.ErrorResponse;
import com.orderflow.common.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ErrorResponse> handleInventory(InsufficientInventoryException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleStateTransition(InvalidStateTransitionException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler({
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class
    })
    public ResponseEntity<ErrorResponse> handleOptimisticLock(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflict", "The resource was updated concurrently by another transaction. Please retry.", req.getRequestURI(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", "Access denied", req.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> ErrorResponse.FieldError.builder()
                        .field(e.getField())
                        .message(e.getDefaultMessage())
                        .build())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation Failed", "Request validation failed", req.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", req.getRequestURI(), null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message,
                                                 String path, List<ErrorResponse.FieldError> fieldErrors) {
        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
