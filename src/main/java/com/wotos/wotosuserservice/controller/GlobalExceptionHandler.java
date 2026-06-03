package com.wotos.wotosuserservice.controller;

import com.wotos.wotosuserservice.model.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Translates registration failures into the standard {@link ErrorResponse} envelope.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Bean-validation failures (e.g. username/password length) -> 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "validation failed";
        }
        return ResponseEntity.badRequest().body(new ErrorResponse("validation_error", message));
    }

    /** Unique-constraint violation on username -> 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("username_taken", "username is already taken"));
    }
}
