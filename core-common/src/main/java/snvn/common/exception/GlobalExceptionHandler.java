package snvn.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import snvn.common.logging.ExternalLogService;
import snvn.common.logging.NoOpExternalLogService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ExternalLogService externalLogService;

    @Autowired(required = false)
    public GlobalExceptionHandler(ExternalLogService externalLogService) {
        this.externalLogService = externalLogService != null ? externalLogService : new NoOpExternalLogService();
        log.info("GlobalExceptionHandler initialized with: {}", this.externalLogService.getClass().getSimpleName());
    }

    public GlobalExceptionHandler() {
        this.externalLogService = new NoOpExternalLogService();
        log.info("GlobalExceptionHandler initialized with NoOpExternalLogService (default)");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        log.error("Configuration error: {}", ex.getMessage(), ex);
        externalLogService.sendErrorLogFile("Configuration Error", ex.toString(), buildContext());
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Configuration Error", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage(), ex);
        externalLogService.sendErrorLogFile("Invalid Argument", ex.toString(), buildContext());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Request", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        String traceId = MDC.get("traceId");
        String correlationId = MDC.get("correlationId");
        log.error("Unhandled exception - traceId={}, correlationId={}", traceId, correlationId, ex);
        externalLogService.sendErrorLogFile("Unhandled Exception", ex.toString(), buildContext());
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage());
    }

    private Map<String, Object> buildContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("traceId", MDC.get("traceId"));
        context.put("spanId", MDC.get("spanId"));
        context.put("correlationId", MDC.get("correlationId"));
        context.put("jobId", MDC.get("jobId"));
        return context;
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        response.put("logger", log.getName() + "." + new Throwable().getStackTrace()[0].getMethodName() + "()");

        response.putAll(buildContext());

        return ResponseEntity.status(status).body(response);
    }
}