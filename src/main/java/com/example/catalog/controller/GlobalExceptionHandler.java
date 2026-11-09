package com.example.catalog.controller;

import com.example.catalog.assistant.AssistantUnavailableException;
import com.example.catalog.service.DownstreamServiceException;
import com.example.catalog.service.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Replaces REACTIVE's per-handler onErrorResume / switchIfEmpty. REACTIVE's
 * 404s had no body; a ProblemDetail is returned instead. REACTIVE's 400 body
 * was {"error": "<message>"}, so 400 ProblemDetails carry the same top-level
 * "error" property.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(ProductNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setProperty("error", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        String message = errors.entrySet().stream()
            .map(e -> e.getKey() + " " + e.getValue())
            .collect(Collectors.joining("; "));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        problem.setProperty("error", message);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(DownstreamServiceException.class)
    public ProblemDetail handleDownstream(DownstreamServiceException ex) {
        log.warn("Downstream failure: {}", ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    /**
     * REACTIVE deliberately returned 500 with the plain-text body
     * "Assistant unavailable: <message>" labeled application/json. That
     * contract is kept rather than switching to a 503 ProblemDetail.
     */
    @ExceptionHandler(AssistantUnavailableException.class)
    public ResponseEntity<String> handleAssistantUnavailable(AssistantUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body("Assistant unavailable: " + ex.getMessage());
    }
}
