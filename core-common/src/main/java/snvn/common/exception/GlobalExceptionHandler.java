package snvn.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Base global exception handler for servlet-based services.
 * Handles common exceptions with proper MDC trace context.
 *
 * For WebClient-specific exception handling (WebClientRequestException, etc.),
 * see MainServiceExceptionHandler in main-service module.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle IllegalStateException (e.g., configuration errors)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        log.error("Configuration error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Configuration Error", ex.getMessage());
    }

    /**
     * Handle IllegalArgumentException (e.g., invalid input)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Request", ex.getMessage());
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        String traceId = MDC.get("traceId");
        String correlationId = MDC.get("correlationId");

        log.error("Unhandled exception occurred - traceId={}, correlationId={}", traceId, correlationId, ex);

        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage());
    }

    /**
     * Build standardized error response with trace context from MDC
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String error,
            String message) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        response.put("traceId", MDC.get("traceId") != null ? MDC.get("traceId") : "");
        response.put("spanId", MDC.get("spanId") != null ? MDC.get("spanId") : "");
        response.put("correlationId", MDC.get("correlationId") != null ? MDC.get("correlationId") : "");
        response.put("jobId", MDC.get("jobId") != null ? MDC.get("jobId") : "");

        return ResponseEntity.status(status).body(response);
    }
}
