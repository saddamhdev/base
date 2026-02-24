package snvn.gatewayservice.dto;

import java.time.Instant;

public class GatewayFallbackResponse {

    private boolean success;
    private String service;
    private int status;
    private String message;
    private String path;
    private String method;
    private String traceId;
    private String spanId;
    private String correlationId;
    private Instant timestamp;

    // constructor
    public GatewayFallbackResponse(boolean success,
                                   String service,
                                   int status,
                                   String message,
                                   String path,
                                   String method,
                                   String traceId,
                                   String spanId,
                                   String correlationId) {
        this.success = success;
        this.service = service;
        this.status = status;
        this.message = message;
        this.path = path;
        this.method = method;
        this.traceId = traceId;
        this.spanId = spanId;
        this.correlationId = correlationId;
        this.timestamp = Instant.now();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
// getters
}