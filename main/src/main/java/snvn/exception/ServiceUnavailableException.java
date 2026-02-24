package snvn.exception;

import org.slf4j.MDC;

/**
 * Exception thrown when a downstream service is unavailable.
 * Captures MDC trace context at construction time to ensure it's available
 * even when the exception is handled in a different thread.
 *
 * This class is in main-service because it's used specifically for
 * WebClient inter-service communication errors.
 */
public class ServiceUnavailableException extends RuntimeException {

    private final String serviceName;
    private final String traceId;
    private final String spanId;
    private final String correlationId;
    private final String jobId;

    public ServiceUnavailableException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
        // Capture MDC context at construction time
        this.traceId = MDC.get("traceId");
        this.spanId = MDC.get("spanId");
        this.correlationId = MDC.get("correlationId");
        this.jobId = MDC.get("jobId");
    }

    public ServiceUnavailableException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        // Capture MDC context at construction time
        this.traceId = MDC.get("traceId");
        this.spanId = MDC.get("spanId");
        this.correlationId = MDC.get("correlationId");
        this.jobId = MDC.get("jobId");
    }

    /**
     * Constructor with explicit trace context (useful when creating from reactive context)
     */
    public ServiceUnavailableException(String serviceName, String message, Throwable cause,
                                       String traceId, String spanId, String correlationId, String jobId) {
        super(message, cause);
        this.serviceName = serviceName;
        this.traceId = traceId;
        this.spanId = spanId;
        this.correlationId = correlationId;
        this.jobId = jobId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getJobId() {
        return jobId;
    }

    @Override
    public String toString() {
        return String.format("ServiceUnavailableException{service='%s', traceId='%s', correlationId='%s', message='%s'}",
                serviceName, traceId, correlationId, getMessage());
    }
}

