package snvn.gatewayservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import snvn.gatewayservice.dto.GatewayFallbackResponse;

import java.net.URI;

@RestController
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/fallback")
    public ResponseEntity<GatewayFallbackResponse> fallback(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        // Extract trace context from headers
        String traceId = getHeaderValue(request, "X-Trace-Id", "X-B3-TraceId");
        String spanId = getHeaderValue(request, "X-Span-Id", "X-B3-SpanId");
        String correlationId = request.getHeaders().getFirst("X-Correlation-Id");

        // Set MDC context for logging BEFORE any logging happens
        if (traceId != null) MDC.put("traceId", traceId);
        if (spanId != null) MDC.put("spanId", spanId);
        if (correlationId != null) MDC.put("correlationId", correlationId);

        try {
            // Get original request path (before forward to fallback)
            URI originalUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            String originalPath = originalUri != null ? originalUri.getPath() : request.getURI().getPath();
            String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";

            // Get the exception that caused the fallback (if available)
            Throwable exception = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
            String errorMessage = exception != null ? exception.getMessage() : "Service unavailable";

            // Log the fallback with full trace context in MDC
            log.error("Fallback triggered - service=main-service, originalPath={}, method={}, error={}",
                    originalPath, method, errorMessage);

            GatewayFallbackResponse response =
                    new GatewayFallbackResponse(
                            false,
                            "main-service",
                            HttpStatus.SERVICE_UNAVAILABLE.value(),
                            "Main Service temporarily unavailable: " + errorMessage,
                            originalPath,
                            method,
                            traceId,
                            spanId,
                            correlationId
                    );

            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(response);
        } finally {
            // Clear MDC after request
            MDC.remove("traceId");
            MDC.remove("spanId");
            MDC.remove("correlationId");
        }
    }

    /**
     * Get header value with fallback to alternate header name
     */
    private String getHeaderValue(ServerHttpRequest request, String... headerNames) {
        for (String headerName : headerNames) {
            String value = request.getHeaders().getFirst(headerName);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }
}