package com.codesage.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for all REST controllers.
 * Provides consistent error responses across the application.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(WebhookValidationException.class)
    public ResponseEntity<Map<String, Object>> handleWebhookValidationException(
            WebhookValidationException ex, WebRequest request) {
        log.error("Webhook validation failed: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Webhook Validation Failed",
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<Map<String, Object>> handleGitHubApiException(
            GitHubApiException ex, WebRequest request) {
        log.error("GitHub API error: {}", ex.getMessage(), ex);
        return buildErrorResponse(
                HttpStatus.BAD_GATEWAY,
                "GitHub API Error",
                "The GitHub operation failed.",
                request);
    }

    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAIServiceException(
            AIServiceException ex, WebRequest request) {
        log.error("AI Service error: {}", ex.getMessage(), ex);
        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI Service Error",
                "The AI analysis operation failed.",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, status);
    }
}
