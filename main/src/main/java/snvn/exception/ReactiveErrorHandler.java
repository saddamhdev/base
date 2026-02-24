package snvn.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for handling WebClient errors in reactive streams.
 * Captures MDC context and wraps exceptions with trace information.
 *
 * This class is in main-service because it uses reactive types (Mono, WebClient exceptions)
 * which are specific to services using WebClient for inter-service communication.
 */
public class ReactiveErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ReactiveErrorHandler.class);

    private ReactiveErrorHandler() {
        // Utility class
    }

    /**
     * Creates an error handler that wraps WebClient exceptions with trace context.
     * Use this in onErrorMap or onErrorResume operators.
     *
     * @param serviceName Name of the service being called
     * @return Function that maps exceptions to ServiceUnavailableException with trace context
     */
    public static Function<Throwable, Throwable> wrapWithTraceContext(String serviceName) {
        // Capture MDC context when the function is created (on the original thread)
        final String traceId = MDC.get("traceId");
        final String spanId = MDC.get("spanId");
        final String correlationId = MDC.get("correlationId");
        final String jobId = MDC.get("jobId");

        return error -> {
            // Restore MDC context on the reactive thread
            restoreMdcContext(traceId, spanId, correlationId, jobId);

            if (error instanceof WebClientRequestException wcError) {
                String uri = wcError.getUri() != null ? wcError.getUri().toString() : "unknown";
                String message = String.format("Service '%s' (%s) is unavailable: %s",
                        serviceName, uri, getRootCauseMessage(error));

                log.error("Service call failed - service={}, traceId={}, correlationId={}, error={}",
                        serviceName, traceId, correlationId, message);

                return new ServiceUnavailableException(serviceName, message, error,
                        traceId, spanId, correlationId, jobId);
            }

            if (error instanceof WebClientResponseException wcError) {
                String message = String.format("Service '%s' returned error: %d - %s",
                        serviceName, wcError.getStatusCode().value(), wcError.getResponseBodyAsString());

                log.error("Service response error - service={}, traceId={}, correlationId={}, status={}, body={}",
                        serviceName, traceId, correlationId,
                        wcError.getStatusCode(), wcError.getResponseBodyAsString());

                return new ServiceUnavailableException(serviceName, message, error,
                        traceId, spanId, correlationId, jobId);
            }

            // For other exceptions, wrap with trace context
            log.error("Unexpected error calling service {} - traceId={}, correlationId={}",
                    serviceName, traceId, correlationId, error);

            return new ServiceUnavailableException(serviceName, error.getMessage(), error,
                    traceId, spanId, correlationId, jobId);
        };
    }

    /**
     * Creates a Mono error handler that returns a fallback value on error.
     * Logs the error with trace context before returning the fallback.
     *
     * @param serviceName Name of the service being called
     * @param fallback    Fallback value to return on error
     * @param <T>         Type of the fallback value
     * @return Function that handles errors and returns fallback
     */
    public static <T> Function<Throwable, Mono<T>> handleWithFallback(String serviceName, T fallback) {
        final String traceId = MDC.get("traceId");
        final String spanId = MDC.get("spanId");
        final String correlationId = MDC.get("correlationId");
        final String jobId = MDC.get("jobId");

        return error -> {
            restoreMdcContext(traceId, spanId, correlationId, jobId);

            log.error("Service '{}' failed, using fallback - traceId={}, correlationId={}, error={}",
                    serviceName, traceId, correlationId, error.getMessage());

            return Mono.just(fallback);
        };
    }

    /**
     * Creates an error handler that propagates the error with trace context.
     * Use when you want to let the GlobalExceptionHandler handle it.
     *
     * @param serviceName Name of the service being called
     * @param <T>         Type parameter for Mono
     * @return Function that wraps and propagates the error
     */
    public static <T> Function<Throwable, Mono<T>> propagateWithTraceContext(String serviceName) {
        final String traceId = MDC.get("traceId");
        final String spanId = MDC.get("spanId");
        final String correlationId = MDC.get("correlationId");
        final String jobId = MDC.get("jobId");

        return error -> {
            restoreMdcContext(traceId, spanId, correlationId, jobId);

            ServiceUnavailableException wrappedException = new ServiceUnavailableException(
                    serviceName,
                    String.format("Service '%s' call failed: %s", serviceName, getRootCauseMessage(error)),
                    error,
                    traceId, spanId, correlationId, jobId
            );

            return Mono.error(wrappedException);
        };
    }

    /**
     * Restores MDC context on the current thread.
     */
    private static void restoreMdcContext(String traceId, String spanId, String correlationId, String jobId) {
        if (traceId != null) MDC.put("traceId", traceId);
        if (spanId != null) MDC.put("spanId", spanId);
        if (correlationId != null) MDC.put("correlationId", correlationId);
        if (jobId != null) MDC.put("jobId", jobId);
    }

    /**
     * Gets the root cause message from the exception chain.
     */
    private static String getRootCauseMessage(Throwable ex) {
        Throwable rootCause = ex;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.getClass().getSimpleName();
    }

    /**
     * Captures current MDC context as a Map.
     * Use this to pass context to reactive streams.
     */
    public static Map<String, String> captureMdcContext() {
        return Map.of(
                "traceId", MDC.get("traceId") != null ? MDC.get("traceId") : "",
                "spanId", MDC.get("spanId") != null ? MDC.get("spanId") : "",
                "correlationId", MDC.get("correlationId") != null ? MDC.get("correlationId") : "",
                "jobId", MDC.get("jobId") != null ? MDC.get("jobId") : ""
        );
    }

    /**
     * Restores MDC context from a captured Map.
     */
    public static void restoreMdcContext(Map<String, String> context) {
        if (context != null) {
            context.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    MDC.put(key, value);
                }
            });
        }
    }
}

