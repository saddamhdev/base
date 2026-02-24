package snvn.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.ConnectException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for main-service.
 * Handles WebClient exceptions and service unavailable errors with proper trace context.
 */
@RestControllerAdvice
public class MainServiceExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MainServiceExceptionHandler.class);

    /**
     * Handle ServiceUnavailableException (custom exception with embedded trace context)
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(ServiceUnavailableException ex) {
        // Use trace context from exception (captured at creation time)
        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                ex.getMessage(),
                ex.getTraceId(),
                ex.getSpanId(),
                ex.getCorrelationId(),
                ex.getJobId(),
                ex.getServiceName()
        );
    }

    /**
     * Handle WebClient request exceptions (connection errors, timeouts)
     */
    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientRequestException(WebClientRequestException ex) {
        String serviceName = extractServiceNameFromUri(ex.getUri() != null ? ex.getUri().toString() : "unknown");
        String message = String.format("Service '%s' is unavailable: %s", serviceName, getRootCauseMessage(ex));

        log.error("WebClient request failed for service: {} - {}", serviceName, message, ex);

        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                message,
                MDC.get("traceId"),
                MDC.get("spanId"),
                MDC.get("correlationId"),
                MDC.get("jobId"),
                serviceName
        );
    }

    /**
     * Handle WebClient response exceptions (4xx, 5xx errors from downstream services)
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientResponseException(WebClientResponseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String message = String.format("Downstream service error: %s - %s",
                ex.getStatusCode(), ex.getResponseBodyAsString());

        log.error("WebClient response error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);

        return buildErrorResponse(
                status.is4xxClientError() ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY,
                status.is4xxClientError() ? "Bad Request" : "Bad Gateway",
                message,
                MDC.get("traceId"),
                MDC.get("spanId"),
                MDC.get("correlationId"),
                MDC.get("jobId"),
                "downstream-service"
        );
    }

    /**
     * Handle connection exceptions
     */
    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<Map<String, Object>> handleConnectException(ConnectException ex) {
        String message = "Unable to connect to downstream service: " + ex.getMessage();

        log.error("Connection failed: {}", message, ex);

        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                message,
                MDC.get("traceId"),
                MDC.get("spanId"),
                MDC.get("correlationId"),
                MDC.get("jobId"),
                "unknown"
        );
    }

    /**
     * Handle IllegalStateException (e.g., "No service client configured")
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        log.error("Configuration error: {}", ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Configuration Error",
                ex.getMessage(),
                MDC.get("traceId"),
                MDC.get("spanId"),
                MDC.get("correlationId"),
                MDC.get("jobId"),
                null
        );
    }

    /**
     * Handle IllegalArgumentException (e.g., "No service client configured for: xxx")
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Request",
                ex.getMessage(),
                MDC.get("traceId"),
                MDC.get("spanId"),
                MDC.get("correlationId"),
                MDC.get("jobId"),
                null
        );
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        String traceId = MDC.get("traceId");
        String correlationId = MDC.get("correlationId");

        log.error("Unhandled exception occurred - traceId={}, correlationId={}", traceId, correlationId, ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                ex.getMessage(),
                traceId,
                MDC.get("spanId"),
                correlationId,
                MDC.get("jobId"),
                null
        );
    }

    /**
     * Build standardized error response with trace context
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            String traceId,
            String spanId,
            String correlationId,
            String jobId,
            String serviceName) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        response.put("traceId", traceId != null ? traceId : "");
        response.put("spanId", spanId != null ? spanId : "");
        response.put("correlationId", correlationId != null ? correlationId : "");
        response.put("jobId", jobId != null ? jobId : "");
        if (serviceName != null) {
            response.put("serviceName", serviceName);
        }

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Extract service name from URI for better error messages
     */
    private String extractServiceNameFromUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "unknown";
        }
        try {
            java.net.URI parsedUri = java.net.URI.create(uri);
            String host = parsedUri.getHost();
            int port = parsedUri.getPort();
            return host + (port > 0 ? ":" + port : "");
        } catch (Exception e) {
            return uri;
        }
    }

    /**
     * Get root cause message from exception chain
     */
    private String getRootCauseMessage(Throwable ex) {
        Throwable rootCause = ex;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.getClass().getSimpleName();
    }
}

